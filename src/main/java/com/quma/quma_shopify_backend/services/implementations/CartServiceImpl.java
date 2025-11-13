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

        boolean cartChanged = false;

        Optional<CartItemDTO> existingOpt = cartItems.stream()
                .filter(c -> c.getProductId().equals(item.getProductId()) &&
                        c.getIdentifier().equals(item.getIdentifier()))
                .findFirst();

        int newQty;
        Product product = productRepository.findByProductId(item.getProductId())
                .orElseThrow(() -> new ApiException("Product not found", 404));
        Variant variant = product.getVariants().stream()
                .filter(v -> v.getIdentifier().equals(item.getIdentifier()))
                .findFirst()
                .orElseThrow(() -> new ApiException("Variant not found", 404));

        newQty = Math.min(item.getQuantity(), Math.min(variant.getStock(), 5));
        boolean isNewlyAdded = false;

        if (existingOpt.isPresent()) {
            CartItemDTO existing = existingOpt.get();
            int delta = newQty - existing.getQuantity();
            existing.setQuantity(newQty);
            cartChanged = true;
            variant.setStock(variant.getStock() - delta);
        } else {
            CartItemDTO newItem = new CartItemDTO();
            newItem.setProductId(product.getProductId());
            newItem.setIdentifier(variant.getIdentifier());
            newItem.setCategories(product.getCategories());
            newItem.setQuantity(newQty);
            cartItems.add(newItem);
            cartChanged = true;
            variant.setStock(variant.getStock() - newQty);
            isNewlyAdded = true;
        }

        productRepository.save(product);

        List<CartItemDTO> enrichedItems = cartItems.stream().map(ci -> {
            Product p = productRepository.findByProductId(ci.getProductId())
                    .orElseThrow(() -> new ApiException("Product not found: " + ci.getProductId(), 404));
            Variant v = p.getVariants().stream()
                    .filter(vv -> vv.getIdentifier().equals(ci.getIdentifier()))
                    .findFirst()
                    .orElseThrow(() -> new ApiException("Variant not found: " + ci.getIdentifier(), 404));

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
            dto.setMaterials(v.getMaterials());
            dto.setWeight(v.getWeight());
            dto.setQuantity(ci.getQuantity());
            dto.setCategories(p.getCategories());
            return dto;
        }).collect(Collectors.toList());

        BigDecimal totalAmount = enrichedItems.stream()
                .map(ci -> (ci.getPrice() != null ? ci.getPrice() : BigDecimal.ZERO)
                        .multiply(BigDecimal.valueOf(ci.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        cart.setCartItemDTOs(enrichedItems);
        cart.setTotalAmount(totalAmount);
        cart.setUpdatedAt(Instant.now());
        cartRepository.save(cart);

        // ✅ Fire async analytics event only for newly added item
        if (isNewlyAdded) {
            ProductAnalyticRequest analyticRequest = new ProductAnalyticRequest();
            analyticRequest.setUsername(username);
            analyticRequest.setProductId(product.getProductId());
            analyticRequest.setIdentifier(variant.getIdentifier());
            analyticRequest.setImageUrl(variant.getImages().isEmpty() ? null : variant.getImages().get(0));
            analyticRequest.setEventType(AnalyticsEventType.ADDED_TO_CART);
            analyticRequest.setCategories(product.getCategories());
            analyticService.recordEvents(List.of(analyticRequest));
        }

        CartResponseDTO response = new CartResponseDTO();
        response.setCartItemDTOs(enrichedItems);
        response.setTotalAmount(totalAmount);
        response.setCartChanged(cartChanged);
        return response;
    }

    @Override
    public CartResponseDTO removeItem(CartItemRequestDTO item) {
        String username = UserContext.get().getUsername();
        CartItemMongoDTO cart = cartRepository.findByUsername(username);
        if (cart == null || CollectionUtils.isEmpty(cart.getCartItemDTOs())) {
            CartResponseDTO emptyResponse = new CartResponseDTO();
            emptyResponse.setCartItemDTOs(Collections.emptyList());
            emptyResponse.setTotalAmount(BigDecimal.ZERO);
            emptyResponse.setCartChanged(false);
            return emptyResponse;
        }

        cart.getCartItemDTOs().removeIf(i -> {
            if (i.getProductId().equals(item.getProductId()) &&
                    i.getIdentifier().equals(item.getIdentifier())) {
                Product p = productRepository.findByProductId(i.getProductId())
                        .orElseThrow(() -> new ApiException("Product not found: " + i.getProductId(), 404));
                Variant v = p.getVariants().stream()
                        .filter(var -> var.getIdentifier().equals(i.getIdentifier()))
                        .findFirst()
                        .orElseThrow(() -> new ApiException("Variant not found: " + i.getIdentifier(), 404));
                v.setStock(v.getStock() + i.getQuantity());
                productRepository.save(p);
                return true;
            }
            return false;
        });

        if (cart.getCartItemDTOs().isEmpty()) {
            cartRepository.delete(cart);
            CartResponseDTO emptyResponse = new CartResponseDTO();
            emptyResponse.setCartItemDTOs(Collections.emptyList());
            emptyResponse.setTotalAmount(BigDecimal.ZERO);
            emptyResponse.setCartChanged(false);
            return emptyResponse;
        }

        List<CartItemDTO> fullCartItems = cart.getCartItemDTOs().stream().map(ci -> {
            Product product = productRepository.findByProductId(ci.getProductId())
                    .orElseThrow(() -> new ApiException("Product not found: " + ci.getProductId(), 404));
            Variant variant = product.getVariants().stream()
                    .filter(v -> v.getIdentifier().equals(ci.getIdentifier()))
                    .findFirst()
                    .orElseThrow(() -> new ApiException("Variant not found: " + ci.getIdentifier(), 404));

            CartItemDTO dto = new CartItemDTO();
            dto.setProductId(product.getProductId());
            dto.setIdentifier(variant.getIdentifier());
            dto.setTitle(product.getTitle());
            dto.setDescription(product.getDescription());
            dto.setPrice(variant.getPrice());
            dto.setDiscountedPrice(variant.getDiscountedPrice());
            dto.setColor(variant.getColor());
            dto.setSize(variant.getSize());
            dto.setImageUrl(variant.getImages().isEmpty() ? null : variant.getImages().get(0));
            dto.setStock(variant.getStock());
            dto.setMaterials(variant.getMaterials());
            dto.setWeight(variant.getWeight());
            dto.setQuantity(ci.getQuantity());
            return dto;
        }).collect(Collectors.toList());

        BigDecimal total = fullCartItems.stream()
                .map(ci -> (ci.getPrice() != null ? ci.getPrice() : BigDecimal.ZERO)
                        .multiply(BigDecimal.valueOf(ci.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        cart.setCartItemDTOs(cart.getCartItemDTOs());
        cart.setTotalAmount(total);
        cart.setUpdatedAt(Instant.now());
        cartRepository.save(cart);

        CartResponseDTO response = new CartResponseDTO();
        response.setCartItemDTOs(fullCartItems);
        response.setTotalAmount(total);
        response.setCartChanged(false);
        return response;
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
                        .cartChanged(false)
                        .build();
            }
            return CartResponseDTO.builder()
                    .cartItemDTOs(cart.getCartItemDTOs())
                    .totalAmount(cart.getTotalAmount() != null ? cart.getTotalAmount() : BigDecimal.ZERO)
                    .cartChanged(false)
                    .build();
        }

        Map<String, CartItemDTO> existingMap = new HashMap<>();
        if (cart != null && cart.getCartItemDTOs() != null) {
            for (CartItemDTO ci : cart.getCartItemDTOs()) {
                existingMap.put(ci.getProductId() + "-" + ci.getIdentifier(), ci);
            }
        } else {
            cart = new CartItemMongoDTO();
            cart.setUsername(username);
            cart.setCartItemDTOs(new ArrayList<>());
        }

        boolean cartChanged = false;
        List<CartItemDTO> mergedItems = new ArrayList<>();
        List<ProductAnalyticRequest> analyticsToRecord = new ArrayList<>();

        Set<String> productIds = items.stream()
                .map(CartItemRequestDTO::getProductId)
                .collect(Collectors.toSet());
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

            int requestedQty = incoming.getQuantity();
            int finalQty = Math.min(requestedQty + existingQty, Math.min(5, variant.getStock() + existingQty));

            if (finalQty != existingQty)
                cartChanged = true;

            variant.setStock(variant.getStock() - (finalQty - existingQty));
            productRepository.save(product);

            if (finalQty <= 0)
                continue;

            // ✅ Analytics event only if item newly added
            if (!existingMap.containsKey(key)) {
                ProductAnalyticRequest analyticRequest = new ProductAnalyticRequest();
                analyticRequest.setUsername(username);
                analyticRequest.setProductId(product.getProductId());
                analyticRequest.setIdentifier(variant.getIdentifier());
                analyticRequest.setImageUrl(variant.getImages().isEmpty() ? null : variant.getImages().get(0));
                analyticRequest.setEventType(AnalyticsEventType.ADDED_TO_CART);
                analyticRequest.setCategories(product.getCategories());
                analyticsToRecord.add(analyticRequest);
            }

            CartItemDTO dto = new CartItemDTO();
            dto.setProductId(product.getProductId());
            dto.setIdentifier(variant.getIdentifier());
            dto.setTitle(product.getTitle());
            dto.setDescription(product.getDescription());
            dto.setPrice(variant.getPrice());
            dto.setDiscountedPrice(variant.getDiscountedPrice());
            dto.setColor(variant.getColor());
            dto.setSize(variant.getSize());
            dto.setImageUrl(variant.getImages().isEmpty() ? null : variant.getImages().get(0));
            dto.setStock(variant.getStock());
            dto.setMaterials(variant.getMaterials());
            dto.setWeight(variant.getWeight());
            dto.setQuantity(finalQty);
            dto.setCategories(product.getCategories());

            mergedItems.add(dto);
            existingMap.remove(key);
        }

        mergedItems.addAll(existingMap.values());

        BigDecimal total = mergedItems.stream()
                .map(i -> (i.getPrice() != null ? i.getPrice() : BigDecimal.ZERO)
                        .multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        cart.setCartItemDTOs(mergedItems);
        cart.setTotalAmount(total);
        cart.setUpdatedAt(Instant.now());
        cartRepository.save(cart);

        // ✅ Fire analytics async
        if (!analyticsToRecord.isEmpty()) {
            analyticService.recordEvents(analyticsToRecord);
            log.info("Triggered {} async analytics events for added-to-cart items", analyticsToRecord.size());
        }

        return CartResponseDTO.builder()
                .cartItemDTOs(mergedItems)
                .totalAmount(total)
                .cartChanged(cartChanged)
                .build();
    }

    @Override
    public boolean checkout(CartCheckoutRequestDTO request) {
        return true;
    }
}
