package com.quma.quma_shopify_backend.services.implementations;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quma.quma_shopify_backend.exceptions.ApiException;
import com.quma.quma_shopify_backend.interfaces.ICartService;
import com.quma.quma_shopify_backend.models.dtos.CartCheckoutRequestDTO;
import com.quma.quma_shopify_backend.models.dtos.CartItemDTO;
import com.quma.quma_shopify_backend.models.dtos.CartItemRequestDTO;
import com.quma.quma_shopify_backend.models.dtos.CartResponseDTO;
import com.quma.quma_shopify_backend.models.mongo.CartItemMongoDTO;
import com.quma.quma_shopify_backend.models.mongo.Product;
import com.quma.quma_shopify_backend.models.mongo.ProductInventoryDTO;
import com.quma.quma_shopify_backend.models.mongo.Variant;
import com.quma.quma_shopify_backend.repositories.mongo.CartRepository;
import com.quma.quma_shopify_backend.repositories.mongo.InventoryRepository;
import com.quma.quma_shopify_backend.repositories.mongo.ProductRepository;
import com.quma.quma_shopify_backend.utilities.UserContext;

@Service
public class CartServiceImpl implements ICartService {

    @Autowired
    InventoryRepository inventoryRepository;

    @Autowired
    CartRepository cartRepository;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    ObjectMapper objectMapper;

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

        // Fetch or create user's cart
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

        // Check if item already exists
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
        if (existingOpt.isPresent()) {
            CartItemDTO existing = existingOpt.get();
            existing.setQuantity(newQty);
            cartChanged = true;
        } else {
            CartItemDTO newItem = new CartItemDTO();
            newItem.setProductId(product.getProductId());
            newItem.setIdentifier(variant.getIdentifier());
            newItem.setQuantity(newQty);
            cartItems.add(newItem);
            cartChanged = true;
        }

        // Build full enriched cart items (like bulkUpdate)
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
            return dto;
        }).collect(Collectors.toList());

        // Update total amount & save
        BigDecimal totalAmount = enrichedItems.stream()
                .map(ci -> (ci.getPrice() != null ? ci.getPrice() : BigDecimal.ZERO)
                        .multiply(BigDecimal.valueOf(ci.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        cart.setCartItemDTOs(enrichedItems);
        cart.setTotalAmount(totalAmount);
        cart.setUpdatedAt(Instant.now());
        cartRepository.save(cart);

        // Build response
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
            // Return empty response instead of null
            CartResponseDTO emptyResponse = new CartResponseDTO();
            emptyResponse.setCartItemDTOs(Collections.emptyList());
            emptyResponse.setTotalAmount(BigDecimal.ZERO);
            emptyResponse.setCartChanged(false);
            return emptyResponse;
        }

        // Remove matching item
        cart.getCartItemDTOs().removeIf(i -> i.getProductId().equals(item.getProductId()) &&
                i.getIdentifier().equals(item.getIdentifier()));

        // If no items left → delete entire cart
        if (cart.getCartItemDTOs().isEmpty()) {
            cartRepository.delete(cart);
            CartResponseDTO emptyResponse = new CartResponseDTO();
            emptyResponse.setCartItemDTOs(Collections.emptyList());
            emptyResponse.setTotalAmount(BigDecimal.ZERO);
            emptyResponse.setCartChanged(false);
            return emptyResponse;
        }

        // Rebuild full frontend-compatible cart items
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

        // Update total amount & save
        BigDecimal total = fullCartItems.stream()
                .map(ci -> (ci.getPrice() != null ? ci.getPrice() : BigDecimal.ZERO)
                        .multiply(BigDecimal.valueOf(ci.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        cart.setCartItemDTOs(cart.getCartItemDTOs());
        cart.setTotalAmount(total);
        cart.setUpdatedAt(Instant.now());
        cartRepository.save(cart);

        // Build response
        CartResponseDTO response = new CartResponseDTO();
        response.setCartItemDTOs(fullCartItems);
        response.setTotalAmount(total);
        response.setCartChanged(false);

        return response;
    }

    @Override
    public CartResponseDTO bulkUpdate(List<CartItemRequestDTO> items) {
        String username = UserContext.get().getUsername();

        if (CollectionUtils.isEmpty(items)) {
            // Clear cart if empty payload
            CartItemMongoDTO existing = cartRepository.findByUsername(username);
            if (existing != null)
                cartRepository.delete(existing);

            CartResponseDTO emptyResponse = new CartResponseDTO();
            emptyResponse.setCartItemDTOs(Collections.emptyList());
            emptyResponse.setTotalAmount(BigDecimal.ZERO);
            emptyResponse.setCartChanged(false);
            return emptyResponse;
        }

        // Fetch or create user's cart
        CartItemMongoDTO cart = cartRepository.findByUsername(username);
        if (cart == null) {
            cart = new CartItemMongoDTO();
            cart.setUsername(username);
            cart.setCartItemDTOs(new ArrayList<>());
        } else {
            cart.getCartItemDTOs().clear(); // overwrite with new bulk items
        }

        boolean cartChanged = false;
        List<CartItemDTO> updatedItems = new ArrayList<>();

        for (CartItemRequestDTO incoming : items) {
            String key = incoming.getProductId() + "-" + incoming.getIdentifier();

            // Validate inventory
            ProductInventoryDTO productInventoryDTO = inventoryRepository.findById(key)
                    .orElseThrow(() -> new ApiException("Product not found in inventory: " + key, 404));

            int available = productInventoryDTO.getAvailableQuantity();
            int maxQty = Math.min(5, available);
            int requestedQty = incoming.getQuantity();

            // enforce limits
            int finalQty = Math.min(requestedQty, maxQty);
            if (finalQty != requestedQty)
                cartChanged = true;

            if (finalQty > 0) {
                // Fetch full product details
                Product product = productRepository.findByProductId(incoming.getProductId())
                        .orElseThrow(() -> new ApiException("Product not found: " + incoming.getProductId(), 404));

                Variant variant = product.getVariants().stream()
                        .filter(v -> v.getIdentifier().equals(incoming.getIdentifier()))
                        .findFirst()
                        .orElseThrow(() -> new ApiException("Variant not found: " + incoming.getIdentifier(), 404));

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

                updatedItems.add(dto);
            }
        }

        // Save updated cart
        BigDecimal total = updatedItems.stream()
                .map(i -> i.getPrice() != null ? i.getPrice().multiply(BigDecimal.valueOf(i.getQuantity()))
                        : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        cart.setCartItemDTOs(updatedItems);
        cart.setTotalAmount(total);
        cart.setUpdatedAt(Instant.now());

        if (updatedItems.isEmpty()) {
            cartRepository.delete(cart);
        } else {
            cartRepository.save(cart);
        }

        // Build response
        CartResponseDTO response = new CartResponseDTO();
        response.setCartItemDTOs(updatedItems);
        response.setTotalAmount(total);
        response.setCartChanged(cartChanged);

        return response;
    }

    @Override
    public boolean checkout(CartCheckoutRequestDTO request) {
        // Temporary reservation logic: reduce inventory for 10 min
        // for (CartItemDTO item : request.getItems()) {
        // String key = item.getProductId() + "-" + item.getIdentifier();
        // int available = inventory.getOrDefault(key, 0);
        // if (available < item.getQuantity()) {
        // return false; // stock insufficient
        // }
        // inventory.put(key, available - item.getQuantity()); // reserve
        // }
        // cart.clear(); // clear cart after reservation
        return true;
    }
}