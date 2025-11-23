package com.quma.quma_shopify_backend.services.implementations;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quma.quma_shopify_backend.enums.AnalyticsEventType;
import com.quma.quma_shopify_backend.exceptions.ApiException;
import com.quma.quma_shopify_backend.interfaces.ICartService;
import com.quma.quma_shopify_backend.models.dtos.*;
import com.quma.quma_shopify_backend.models.mongo.*;
import com.quma.quma_shopify_backend.repositories.mongo.*;
import com.quma.quma_shopify_backend.services.ProductAnalyticService;
import com.quma.quma_shopify_backend.utilities.UserContext;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class CartServiceImpl implements ICartService {

    @Autowired
    InventoryRepository inventoryRepository;

    @Autowired
    CartRepository cartRepository;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    ProductAnalyticService analyticService;

    @Override
    public CartResponseDTO getCartItems() {
        String username = UserContext.get().getUsername();
        CartItemMongoDTO cart = cartRepository.findByUsername(username);

        if (cart == null) {
            CartResponseDTO emptyResponse = new CartResponseDTO();
            emptyResponse.setCartItemDTOs(Collections.emptyList());
            emptyResponse.setTotalAmount(BigDecimal.ZERO);
            return emptyResponse;
        }

        CartResponseDTO response = objectMapper.convertValue(cart, CartResponseDTO.class);
        return response;
    }

    @Override
    public CartResponseDTO addItem(CartItemRequestDTO item) {
        String username = UserContext.get().getUsername();

        CartItemMongoDTO cart = cartRepository.findByUsername(username);
        if (cart == null) {
            cart = new CartItemMongoDTO();
            cart.setUsername(username);
            cart.setCartItemDTOs(new ArrayList<>());
        }

        List<CartItemDTO> cartItems = cart.getCartItemDTOs();
        if (cartItems == null)
            cartItems = new ArrayList<>();

        // Fetch product once
        Product product = productRepository.findByProductId(item.getProductId())
                .orElseThrow(() -> new ApiException("Product not found", 404));

        // Find existing cart item
        Optional<CartItemDTO> existingOpt = cartItems.stream()
                .filter(ci -> ci.getProductId().equals(item.getProductId())
                        && ci.getIdentifier().equals(item.getIdentifier()))
                .findFirst();

        int finalQty;
        boolean isNewlyAdded = false;

        if (existingOpt.isPresent()) {
            CartItemDTO existing = existingOpt.get();
            int requested = existing.getQuantity() + item.getQuantity();
            finalQty = Math.min(requested, 5); // max 5
            existing.setQuantity(finalQty);
        } else {
            finalQty = Math.min(item.getQuantity(), 5);

            CartItemDTO newItem = new CartItemDTO();
            newItem.setProductId(product.getProductId());
            newItem.setIdentifier(item.getIdentifier());
            newItem.setQuantity(finalQty);
            newItem.setCategories(product.getCategories());
            cartItems.add(newItem);

            isNewlyAdded = true; // mark for analytics
        }
        // --- Enrich for response only
        List<CartItemDTO> enrichedItems = enrichCartItems(cartItems);

        cart.setCartItemDTOs(enrichedItems);
        cart.setUpdatedAt(Instant.now());
        cartRepository.save(cart);

        BigDecimal totalAmount = enrichedItems.stream()
                .map(ci -> ci.getPrice().multiply(BigDecimal.valueOf(ci.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // ✅ Analytics for newly added items
        if (isNewlyAdded) {
            ProductAnalyticRequest analyticRequest = new ProductAnalyticRequest();
            analyticRequest.setUsername(username);
            analyticRequest.setProductId(product.getProductId());
            analyticRequest.setIdentifier(item.getIdentifier());
            analyticRequest.setImageUrl(
                    product.getVariants().stream()
                            .filter(v -> v.getIdentifier().equals(item.getIdentifier()))
                            .findFirst()
                            .map(v -> v.getImages().isEmpty() ? null : v.getImages().get(0))
                            .orElse(null));
            analyticRequest.setEventType(AnalyticsEventType.ADDED_TO_CART);
            analyticRequest.setCategories(product.getCategories());
            analyticService.recordEvents(List.of(analyticRequest));
        }

        CartResponseDTO response = new CartResponseDTO();
        response.setCartItemDTOs(enrichedItems);
        response.setTotalAmount(totalAmount);
        response.setCartChanged(true);
        return response;
    }

    private List<CartItemDTO> enrichCartItems(List<CartItemDTO> cartItems) {
        return cartItems.stream().map(ci -> {
            Product p = productRepository.findByProductId(ci.getProductId())
                    .orElseThrow(() -> new ApiException("Product not found", 404));

            Variant v = p.getVariants().stream()
                    .filter(x -> x.getIdentifier().equals(ci.getIdentifier()))
                    .findFirst()
                    .orElseThrow(() -> new ApiException("Variant not found", 404));

            CartItemDTO dto = new CartItemDTO();
            dto.setProductId(p.getProductId());
            dto.setIdentifier(v.getIdentifier());
            dto.setTitle(p.getTitle());
            dto.setDescription(p.getDescription());
            dto.setPrice(v.getPrice());
            dto.setDiscountedPrice(v.getDiscountedPrice());
            dto.setColor(v.getColor());
            dto.setSize(v.getSize());
            dto.setImageUrl(v.getImages().isEmpty() ? null : v.getImages().get(0));
            dto.setStock(v.getStock());
            dto.setQuantity(ci.getQuantity());
            dto.setCategories(p.getCategories());
            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    public CartResponseDTO removeItem(CartItemRequestDTO item) {
        String username = UserContext.get().getUsername();
        CartItemMongoDTO cart = cartRepository.findByUsername(username);

        if (cart == null || CollectionUtils.isEmpty(cart.getCartItemDTOs())) {
            return CartResponseDTO.builder()
                    .cartItemDTOs(Collections.emptyList())
                    .totalAmount(BigDecimal.ZERO)
                    .cartChanged(false)
                    .build();
        }

        // Remove the item
        boolean removed = cart.getCartItemDTOs().removeIf(i -> i.getProductId().equals(item.getProductId()) &&
                i.getIdentifier().equals(item.getIdentifier()));

        if (!removed) {
            // Item not found, return current cart
            List<CartItemDTO> enrichedItems = enrichCartItems(cart.getCartItemDTOs());
            BigDecimal total = calculateTotal(enrichedItems);

            return CartResponseDTO.builder()
                    .cartItemDTOs(enrichedItems)
                    .totalAmount(total)
                    .cartChanged(false)
                    .build();
        }

        if (cart.getCartItemDTOs().isEmpty()) {
            cartRepository.delete(cart);
            return CartResponseDTO.builder()
                    .cartItemDTOs(Collections.emptyList())
                    .totalAmount(BigDecimal.ZERO)
                    .cartChanged(true)
                    .build();
        }

        // Enrich remaining items for response
        List<CartItemDTO> enrichedItems = enrichCartItems(cart.getCartItemDTOs());
        BigDecimal total = calculateTotal(enrichedItems);

        cart.setCartItemDTOs(enrichedItems);
        cart.setTotalAmount(total);
        cart.setUpdatedAt(Instant.now());
        cartRepository.save(cart);

        return CartResponseDTO.builder()
                .cartItemDTOs(enrichedItems)
                .totalAmount(total)
                .cartChanged(true)
                .build();
    }

    private BigDecimal calculateTotal(List<CartItemDTO> items) {
        return items.stream()
                .map(ci -> (ci.getPrice() != null ? ci.getPrice() : BigDecimal.ZERO)
                        .multiply(BigDecimal.valueOf(ci.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public CartResponseDTO bulkUpdate(List<CartItemRequestDTO> items) {
        String username = UserContext.get().getUsername();
        CartItemMongoDTO cart = cartRepository.findByUsername(username);

        if (CollectionUtils.isEmpty(items)) {
            if (cart == null) {
                return CartResponseDTO.builder()
                        .cartItemDTOs(Collections.emptyList())
                        .totalAmount(BigDecimal.ZERO)
                        .build();
            }
            return CartResponseDTO.builder()
                    .cartItemDTOs(cart.getCartItemDTOs() != null ? enrichCartItems(cart.getCartItemDTOs())
                            : Collections.emptyList())
                    .totalAmount(cart.getTotalAmount() != null ? cart.getTotalAmount() : BigDecimal.ZERO)
                    .build();
        }

        if (cart == null) {
            cart = new CartItemMongoDTO();
            cart.setUsername(username);
            cart.setCartItemDTOs(new ArrayList<>());
        }

        Map<String, CartItemDTO> existingMap = cart.getCartItemDTOs().stream()
                .collect(Collectors.toMap(ci -> ci.getProductId() + "-" + ci.getIdentifier(), ci -> ci));

        List<ProductAnalyticRequest> analyticsToRecord = new ArrayList<>();

        // Fetch all products in bulk
        Set<String> productIds = items.stream().map(CartItemRequestDTO::getProductId).collect(Collectors.toSet());
        List<Product> products = productRepository.findAllByProductIdIn(new ArrayList<>(productIds));
        Map<String, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getProductId, p -> p));

        for (CartItemRequestDTO incoming : items) {
            Product product = productMap.get(incoming.getProductId());
            if (product == null)
                continue;

            Variant variant = product.getVariants().stream()
                    .filter(v -> v.getIdentifier().equals(incoming.getIdentifier()))
                    .findFirst()
                    .orElse(null);
            if (variant == null)
                continue;

            String key = incoming.getProductId() + "-" + incoming.getIdentifier();
            int existingQty = existingMap.containsKey(key) ? existingMap.get(key).getQuantity() : 0;

            int finalQty = Math.min(existingQty + incoming.getQuantity(), 5); // max 5 per variant
            if (finalQty <= 0) {
                existingMap.remove(key);
                continue;
            }

            CartItemDTO dto = new CartItemDTO();
            dto.setProductId(product.getProductId());
            dto.setIdentifier(variant.getIdentifier());
            dto.setQuantity(finalQty);
            dto.setCategories(product.getCategories());
            existingMap.put(key, dto);

            // Analytics for newly added items
            if (existingQty == 0) {
                ProductAnalyticRequest analyticRequest = new ProductAnalyticRequest();
                analyticRequest.setUsername(username);
                analyticRequest.setProductId(product.getProductId());
                analyticRequest.setIdentifier(variant.getIdentifier());
                analyticRequest.setImageUrl(
                        variant.getImages().isEmpty() ? null : variant.getImages().get(0));
                analyticRequest.setEventType(AnalyticsEventType.ADDED_TO_CART);
                analyticRequest.setCategories(product.getCategories());
                analyticsToRecord.add(analyticRequest);
            }
        }

        List<CartItemDTO> mergedItems = new ArrayList<>(existingMap.values());

        // Save updated cart
        List<CartItemDTO> enrichedItems = enrichCartItems(mergedItems);
        cart.setCartItemDTOs(enrichedItems);
        cart.setUpdatedAt(Instant.now());
        cartRepository.save(cart);

        // Enrich for response
        BigDecimal total = calculateTotal(enrichedItems);

        // Fire analytics async
        if (!analyticsToRecord.isEmpty()) {
            analyticService.recordEvents(analyticsToRecord);
            log.info("Triggered {} async analytics events for added-to-cart items", analyticsToRecord.size());
        }

        return CartResponseDTO.builder()
                .cartItemDTOs(enrichedItems)
                .totalAmount(total)
                .build();
    }

    @Override
    public CartResponseDTO checkout() {
        String username = UserContext.get().getUsername();
        CartItemMongoDTO cart = cartRepository.findByUsername(username);

        if (cart == null || CollectionUtils.isEmpty(cart.getCartItemDTOs())) {
            return CartResponseDTO.builder()
                    .cartItemDTOs(Collections.emptyList())
                    .totalAmount(BigDecimal.ZERO)
                    .cartChanged(false)
                    .build();
        }

        // Fetch all products in bulk
        Set<String> productIds = cart.getCartItemDTOs().stream()
                .map(CartItemDTO::getProductId)
                .collect(Collectors.toSet());

        List<Product> products = productRepository.findAllByProductIdIn(new ArrayList<>(productIds));
        Map<String, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getProductId, p -> p));

        boolean cartChanged = false;

        // ⭐ NEW LIST after removing zero-qty items
        List<CartItemDTO> updatedItems = new ArrayList<>();

        for (CartItemDTO item : cart.getCartItemDTOs()) {
            Product product = productMap.get(item.getProductId());
            if (product == null)
                continue;

            Variant variant = product.getVariants().stream()
                    .filter(v -> v.getIdentifier().equals(item.getIdentifier()))
                    .findFirst()
                    .orElse(null);
            if (variant == null)
                continue;

            int finalQty = Math.min(item.getQuantity(), Math.min(variant.getStock(), 5));

            if (finalQty != item.getQuantity()) {
                cartChanged = true;
            }

            // ⭐ Remove items with zero quantity
            if (finalQty > 0) {
                item.setQuantity(finalQty);
                updatedItems.add(item);
            } else {
                cartChanged = true; // because an item was removed
            }
        }

        // If cart becomes empty after removals
        if (updatedItems.isEmpty()) {
            cartRepository.delete(cart);
            return CartResponseDTO.builder()
                    .cartItemDTOs(Collections.emptyList())
                    .totalAmount(BigDecimal.ZERO)
                    .cartChanged(true)
                    .build();
        }

        // Enrich items
        List<CartItemDTO> enrichedItems = enrichCartItems(updatedItems);
        cart.setCartItemDTOs(enrichedItems);
        cart.setUpdatedAt(Instant.now());
        cartRepository.save(cart);

        BigDecimal totalAmount = calculateTotal(enrichedItems);

        return CartResponseDTO.builder()
                .cartItemDTOs(enrichedItems)
                .totalAmount(totalAmount)
                .cartChanged(cartChanged)
                .build();
    }

}
