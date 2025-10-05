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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final ProductRepository productRepository;
    private final ObjectMapper objectMapper;

    // ✅ Toggle add/remove single wishlist
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
        } else if (existing != null) {
            wishlistRepository.deleteByUsernameAndProductIdAndIdentifier(username, productId, identifier);
            log.info("Removed wishlist item for user {} -> product {} variant {}", username, productId, identifier);
        }

        return markWishlist ? "added" : "removed";
    }

    // ✅ Paginated wishlist fetch
    public Page<WishlistResponseDTO> getWishlist(int page, int size) {
        String username = UserContext.get().getUsername();
        Pageable pageable = PageRequest.of(page, size);

        Page<Wishlist> wishlistPage = wishlistRepository.findByUsername(username, pageable);

        List<WishlistResponseDTO> wishlistDTOs = wishlistPage.getContent().stream()
                .map(w -> objectMapper.convertValue(w, WishlistResponseDTO.class))
                .collect(Collectors.toList());

        return new PageImpl<>(wishlistDTOs, pageable, wishlistPage.getTotalElements());
    }

    // ✅ Clean duplicate-safe bulk add
    public void bulkAddToWishlist(List<WishlistRequestDTO> requests) {
        if (requests == null || requests.isEmpty())
            return;

        String username = UserContext.get().getUsername();

        // Group by productId for efficient queries
        Map<String, List<String>> productVariantsMap = requests.stream()
                .collect(Collectors.groupingBy(
                        WishlistRequestDTO::getProductId,
                        Collectors.mapping(WishlistRequestDTO::getIdentifier, Collectors.toList())));

        for (Map.Entry<String, List<String>> entry : productVariantsMap.entrySet()) {
            String productId = entry.getKey();
            List<String> identifiers = entry.getValue();

            Product product = productRepository.findByProductId(productId)
                    .orElseThrow(() -> new RuntimeException("Product not found"));

            // Find existing wishlists for this user and product
            List<Wishlist> existing = wishlistRepository.findByUsernameAndProductId(username, productId);
            Set<String> existingIdentifiers = existing.stream()
                    .map(Wishlist::getIdentifier)
                    .collect(Collectors.toSet());

            // Filter only new variants not in wishlist
            List<Wishlist> toSave = product.getVariants().stream()
                    .filter(v -> identifiers.contains(v.getIdentifier())
                            && !existingIdentifiers.contains(v.getIdentifier()))
                    .map(variant -> {
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
                    .collect(Collectors.toList());

            if (!toSave.isEmpty())
                wishlistRepository.saveAll(toSave);
        }

        log.info("Bulk added wishlist items for user {}", username);
    }
}
