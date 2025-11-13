package com.quma.quma_shopify_backend.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quma.quma_shopify_backend.enums.AnalyticsEventType;
import com.quma.quma_shopify_backend.models.dtos.ProductAnalyticRequest;
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
        private final ProductAnalyticService analyticService;

        // ✅ Toggle add/remove single wishlist
        public String toggleWishlist(WishlistRequestDTO request) {
                String username = UserContext.get().getUsername();
                String productId = request.getProductId();
                String identifier = request.getIdentifier();
                boolean markWishlist = request.isMarkWishlist();

                String productIdAndIdentifier = productId + "$" + identifier;
                Wishlist existing = wishlistRepository
                                .findByUsernameAndProductId(username, productIdAndIdentifier)
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
                                wishlistMap.put("productId", productIdAndIdentifier);
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

                                log.info("Added wishlist item for user {} -> product {} variant {}", username,
                                                productId, identifier);

                                // ✅ Fire async analytics event
                                ProductAnalyticRequest analyticRequest = new ProductAnalyticRequest();
                                analyticRequest.setUsername(username);
                                analyticRequest.setProductId(productId);
                                analyticRequest.setIdentifier(identifier);
                                analyticRequest.setImageUrl(firstImage);
                                analyticRequest.setEventType(AnalyticsEventType.FAVORITE);
                                analyticRequest.setCategories(product.getCategories());

                                analyticService.recordEvents(List.of(analyticRequest)); // <-- async call
                        }
                } else if (existing != null) {
                        wishlistRepository.deleteByUsernameAndProductId(username, productIdAndIdentifier);
                        log.info("Removed wishlist item for user {} -> product {} variant {}", username, productId,
                                        identifier);
                }

                return markWishlist ? "added" : "removed";
        }

        // ✅ Paginated wishlist fetch
        public Page<WishlistResponseDTO> getWishlist(int page, int size) {
                String username = UserContext.get().getUsername();
                Pageable pageable = PageRequest.of(page, size);

                Page<Wishlist> wishlistPage = wishlistRepository.findByUsername(username, pageable);

                List<WishlistResponseDTO> wishlistDTOs = wishlistPage.getContent().stream()
                                .map(w -> {
                                        WishlistResponseDTO dto = objectMapper.convertValue(w,
                                                        WishlistResponseDTO.class);

                                        // Split combined productId if exists
                                        if (w.getProductId() != null && w.getProductId().contains("$")) {
                                                String[] parts = w.getProductId().split("\\$", 2);
                                                dto.setProductId(parts[0]);
                                                dto.setIdentifier(parts[1]);
                                        } else {
                                                dto.setProductId(w.getProductId());
                                                dto.setIdentifier(w.getIdentifier());
                                        }

                                        return dto;
                                })
                                .collect(Collectors.toList());

                return new PageImpl<>(wishlistDTOs, pageable, wishlistPage.getTotalElements());
        }

        public void bulkAddToWishlist(List<WishlistRequestDTO> requests) {
                if (requests == null || requests.isEmpty())
                        return;

                String username = UserContext.get().getUsername();

                List<String> combinedIds = requests.stream()
                                .map(r -> r.getProductId() + "$" + r.getIdentifier())
                                .toList();

                List<Wishlist> existing = wishlistRepository.findAllByUsernameAndProductIdIn(username, combinedIds);
                Set<String> existingIds = existing.stream()
                                .map(Wishlist::getProductId)
                                .collect(Collectors.toSet());

                Set<String> baseProductIds = requests.stream()
                                .map(WishlistRequestDTO::getProductId)
                                .collect(Collectors.toSet());

                List<Product> products = productRepository.findAllByProductIdIn(baseProductIds.stream().toList());
                Map<String, Product> productMap = products.stream()
                                .collect(Collectors.toMap(Product::getProductId, p -> p));

                List<Wishlist> toAdd = new ArrayList<>();
                List<String> toRemove = new ArrayList<>();
                List<ProductAnalyticRequest> analyticsToRecord = new ArrayList<>();

                for (WishlistRequestDTO req : requests) {
                        String baseProductId = req.getProductId();
                        String identifier = req.getIdentifier();
                        String combinedId = baseProductId + "$" + identifier;
                        boolean markWishlist = req.isMarkWishlist();

                        if (markWishlist) {
                                if (existingIds.contains(combinedId))
                                        continue;

                                Product product = productMap.get(baseProductId);
                                if (product == null)
                                        continue;

                                Variant variant = product.getVariants().stream()
                                                .filter(v -> v.getIdentifier().equals(identifier))
                                                .findFirst()
                                                .orElse(null);

                                if (variant == null)
                                        continue;

                                String firstImage = (variant.getImages() != null && !variant.getImages().isEmpty())
                                                ? variant.getImages().get(0)
                                                : "";

                                Map<String, Object> wishlistMap = new HashMap<>();
                                wishlistMap.put("username", username);
                                wishlistMap.put("productId", combinedId);
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
                                toAdd.add(wishlist);

                                // ✅ Build async analytics request
                                ProductAnalyticRequest analyticRequest = new ProductAnalyticRequest();
                                analyticRequest.setUsername(username);
                                analyticRequest.setProductId(baseProductId);
                                analyticRequest.setIdentifier(identifier);
                                analyticRequest.setImageUrl(firstImage);
                                analyticRequest.setEventType(AnalyticsEventType.FAVORITE);
                                analyticRequest.setCategories(product.getCategories());

                                analyticsToRecord.add(analyticRequest);
                        } else {
                                if (existingIds.contains(combinedId))
                                        toRemove.add(combinedId);
                        }
                }

                // Bulk save and remove
                if (!toAdd.isEmpty())
                        wishlistRepository.saveAll(toAdd);

                if (!toRemove.isEmpty())
                        wishlistRepository.deleteByUsernameAndProductIdIn(username, toRemove);

                log.info("Bulk wishlist update complete for user {}. Added: {}, Removed: {}",
                                username, toAdd.size(), toRemove.size());

                // ✅ Fire async analytics events for all newly added items
                if (!analyticsToRecord.isEmpty()) {
                        analyticService.recordEvents(analyticsToRecord); // <-- async
                        log.info("Triggered {} async analytics events for wishlist additions",
                                        analyticsToRecord.size());
                }
        }

}
