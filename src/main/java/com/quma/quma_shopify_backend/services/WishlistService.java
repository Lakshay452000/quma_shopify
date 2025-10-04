package com.quma.quma_shopify_backend.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quma.quma_shopify_backend.models.dtos.WishlistRequestDTO;
import com.quma.quma_shopify_backend.models.dtos.WishlistResponseDTO;
import com.quma.quma_shopify_backend.models.mongo.Product;
import com.quma.quma_shopify_backend.models.mongo.Variant;
import com.quma.quma_shopify_backend.models.mongo.Wishlist;
import com.quma.quma_shopify_backend.repositories.mongo.ProductRepository;
import com.quma.quma_shopify_backend.repositories.mongo.WishlistRepository;
import com.quma.quma_shopify_backend.utilities.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final ProductRepository productRepository;
    private final ObjectMapper objectMapper;

    public String toggleWishlist(WishlistRequestDTO request) {
        String username = UserContext.get().getUsername();
        String productId = request.getProductId();
        String identifier = request.getIdentifier();
        boolean markWishlist = request.isMarkWishlist();

        Wishlist existing = wishlistRepository
                .findByUsernameAndProductIdAndIdentifier(username, productId, identifier)
                .orElse(null);

        if (markWishlist) {
            if (existing == null) {
                Product product = productRepository.findByProductId(productId)
                        .orElseThrow(() -> new RuntimeException("Product not found"));

                Variant variant = product.getVariants().stream()
                        .filter(v -> v.getIdentifier().equals(identifier))
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("Variant not found"));

                String firstImage = (variant.getImages() != null && !variant.getImages().isEmpty())
                        ? variant.getImages().get(0)
                        : "";

                Map<String, Object> wishlistMap = new HashMap<>();
                wishlistMap.put("username", username);
                wishlistMap.put("productId", productId);
                wishlistMap.put("identifier", identifier);
                wishlistMap.put("title", product.getTitle());
                wishlistMap.put("price", variant.getPrice());
                wishlistMap.put("imageUrl", firstImage);
                wishlistMap.put("brand", product.getBrand());
                wishlistMap.put("rating", product.getAverageRating());
                wishlistMap.put("reviews", product.getTotalReviews());
                wishlistMap.put("color", variant.getColor());
                wishlistMap.put("size", variant.getSize());
                wishlistMap.put("createdAt", Instant.now());

                Wishlist wishlist = objectMapper.convertValue(wishlistMap, Wishlist.class);
                wishlistRepository.save(wishlist);

                log.info("Added wishlist item for user {} -> product {} variant {}", username, productId, identifier);
            }
        } else {
            if (existing != null) {
                wishlistRepository.deleteByUsernameAndProductIdAndIdentifier(username, productId, identifier);
                log.info("Removed wishlist item for user {} -> product {} variant {}", username, productId, identifier);
            }
        }

        return markWishlist ? "added" : "removed";
    }

    public List<WishlistResponseDTO> getWishlist() {
        String username = UserContext.get().getUsername();
        List<Wishlist> wishlists = wishlistRepository.findByUsername(username);

        return wishlists.stream()
                .map(w -> objectMapper.convertValue(w, WishlistResponseDTO.class))
                .collect(Collectors.toList());
    }

    public void bulkAddToWishlist(List<WishlistRequestDTO> requests) {
        if (requests.isEmpty())
            return;

        String username = UserContext.get().getUsername();

        Map<String, List<String>> productVariantsMap = requests.stream()
                .collect(Collectors.groupingBy(
                        WishlistRequestDTO::getProductId,
                        Collectors.mapping(WishlistRequestDTO::getIdentifier, Collectors.toList())));

        for (Map.Entry<String, List<String>> entry : productVariantsMap.entrySet()) {
            String productId = entry.getKey();
            List<String> identifiers = entry.getValue();

            Product product = productRepository.findByProductId(productId)
                    .orElseThrow(() -> new RuntimeException("Product not found"));

            List<Wishlist> existingWishlists = wishlistRepository.findByUsernameAndProductId(username, productId);
            Map<String, Wishlist> existingMap = existingWishlists.stream()
                    .collect(Collectors.toMap(Wishlist::getIdentifier, w -> w));

            List<Wishlist> wishlistsToSave = product.getVariants().stream()
                    .filter(v -> identifiers.contains(v.getIdentifier()))
                    .map(variant -> {
                        if (existingMap.containsKey(variant.getIdentifier()))
                            return null;

                        String firstImage = (variant.getImages() != null && !variant.getImages().isEmpty())
                                ? variant.getImages().get(0)
                                : "";

                        Map<String, Object> wishlistMap = new HashMap<>();
                        wishlistMap.put("username", username);
                        wishlistMap.put("productId", productId);
                        wishlistMap.put("identifier", variant.getIdentifier());
                        wishlistMap.put("title", product.getTitle());
                        wishlistMap.put("price", variant.getPrice());
                        wishlistMap.put("imageUrl", firstImage);
                        wishlistMap.put("brand", product.getBrand());
                        wishlistMap.put("rating", product.getAverageRating());
                        wishlistMap.put("reviews", product.getTotalReviews());
                        wishlistMap.put("color", variant.getColor());
                        wishlistMap.put("size", variant.getSize());
                        wishlistMap.put("createdAt", Instant.now());

                        return objectMapper.convertValue(wishlistMap, Wishlist.class);
                    })
                    .filter(w -> w != null)
                    .collect(Collectors.toList());

            // ✅ No longer update product variants
            if (!wishlistsToSave.isEmpty()) {
                wishlistRepository.saveAll(wishlistsToSave);
            }
        }

        log.info("Bulk added wishlist items for user {}", username);
    }
}
