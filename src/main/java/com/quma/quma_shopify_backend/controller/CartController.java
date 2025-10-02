package com.quma.quma_shopify_backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.quma.quma_shopify_backend.models.dtos.CartCheckoutRequestDTO;
import com.quma.quma_shopify_backend.models.dtos.CartItemRequestDTO;
import com.quma.quma_shopify_backend.models.dtos.CartResponseDTO;
import com.quma.quma_shopify_backend.services.implementations.CartServiceImpl;

import java.util.List;

@RestController
@RequestMapping("/cart")
public class CartController {

    @Autowired
    CartServiceImpl cartService;

    // Fetch all items in cart
    @GetMapping("/get-items")
    public ResponseEntity<CartResponseDTO> getCart() {
        return ResponseEntity.ok(cartService.getCartItems());
    }

    // Add an item to cart
    @PostMapping("/add")
    public ResponseEntity<CartResponseDTO> addToCart(@RequestBody CartItemRequestDTO item) {
        CartResponseDTO updatedCart = cartService.addItem(item);
        return ResponseEntity.ok(updatedCart);
    }

    // Remove item from cart
    @DeleteMapping("/remove")
    public ResponseEntity<CartResponseDTO> removeFromCart(
            @RequestBody CartItemRequestDTO item) {
        CartResponseDTO updatedCart = cartService.removeItem(item);
        return ResponseEntity.ok(updatedCart);
    }

    // Bulk update (used for merging guest cart on login)
    @PostMapping("/bulk-update")
    public ResponseEntity<CartResponseDTO> bulkUpdateCart(
            @RequestBody List<CartItemRequestDTO> items) {
        CartResponseDTO updatedCart = cartService.bulkUpdate(items);
        return ResponseEntity.ok(updatedCart);
    }

    // Checkout (temporary reservation)
    @PostMapping("/checkout")
    public ResponseEntity<String> checkout(@RequestBody CartCheckoutRequestDTO request) {
        boolean success = cartService.checkout(request);
        if (success) {
            return ResponseEntity.ok("Order placed, payment pending. Cart reserved for 10 mins.");
        } else {
            return ResponseEntity.badRequest().body("Some items are out of stock. Please adjust cart.");
        }
    }
}