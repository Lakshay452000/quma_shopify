package com.quma.quma_shopify_backend.services.implementations;

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
            emptyResponse.setTotalAmount(0.0);
            return emptyResponse;
        }

        CartResponseDTO response = objectMapper.convertValue(cart, CartResponseDTO.class);

        return response;
    }

    @Override
    public CartResponseDTO addItem(CartItemRequestDTO item) {
        String username = UserContext.get().getUsername();

        // 1️⃣ Fetch product variant from inventory
        ProductInventoryDTO productInventoryDTO = inventoryRepository
                .findById(item.getProductId() + "-" + item.getIdentifier())
                .orElseThrow(() -> new ApiException("Product not found in inventory", 404));

        int available = productInventoryDTO.getAvailableQuantity();
        int maxQty = Math.min(5, available);

        // 2️⃣ Fetch or create user's cart
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

        // 3️⃣ Check if item already exists
        Optional<CartItemDTO> existingOpt = cartItems.stream()
                .filter(c -> c.getProductId().equals(item.getProductId()) &&
                        c.getIdentifier().equals(item.getIdentifier()))
                .findFirst();

        int newQty;
        if (existingOpt.isPresent()) {
            CartItemDTO existing = existingOpt.get();
            newQty = Math.min(item.getQuantity(), maxQty);

            if (newQty != item.getQuantity()) {
                cartChanged = true;
            }
            existing.setQuantity(newQty);
            existing.setPrice(productInventoryDTO.getPrice()); // optional: keep totalPrice if needed
        } else {
            newQty = Math.min(item.getQuantity(), maxQty);
            if (newQty < item.getQuantity())
                cartChanged = true;

            CartItemDTO newItem = new CartItemDTO();
            newItem.setProductId(item.getProductId());
            newItem.setIdentifier(item.getIdentifier());
            newItem.setQuantity(newQty);
            cartItems.add(newItem);
        }

        // 4️⃣ Update total amount in cart
        double totalAmount = cartItems.stream()
                .mapToDouble(ci -> (ci.getPrice() != null ? ci.getPrice() : 0) * ci.getQuantity())
                .sum();
        cart.setTotalAmount(totalAmount);
        cart.setCartItemDTOs(cartItems);

        // 5️⃣ Save cart
        cartRepository.save(cart);

        // 6️⃣ Build full frontend-compatible response
        List<CartItemDTO> fullCartItems = cartItems.stream().map(ci -> {
            Product product = productRepository.findByProductId(ci.getProductId())
                    .orElseThrow(() -> new ApiException("Product not found", 404));

            Variant variant = product.getVariants().stream()
                    .filter(v -> v.getIdentifier().equals(ci.getIdentifier()))
                    .findFirst()
                    .orElseThrow(() -> new ApiException("Variant not found", 404));

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

        // 7️⃣ Build response
        CartResponseDTO response = new CartResponseDTO();
        response.setCartItemDTOs(fullCartItems);
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
            emptyResponse.setTotalAmount(0.0);
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
            emptyResponse.setTotalAmount(0.0);
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
        double total = fullCartItems.stream()
                .mapToDouble(ci -> ci.getPrice() != null ? ci.getPrice() * ci.getQuantity() : 0)
                .sum();
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
            emptyResponse.setTotalAmount(0.0);
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
        double total = updatedItems.stream()
                .mapToDouble(i -> i.getPrice() != null ? i.getPrice() * i.getQuantity() : 0)
                .sum();

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