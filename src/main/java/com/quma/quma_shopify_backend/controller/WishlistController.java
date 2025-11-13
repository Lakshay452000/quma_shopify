package com.quma.quma_shopify_backend.controller;

import com.quma.quma_shopify_backend.models.dtos.WishlistRequestDTO;
import com.quma.quma_shopify_backend.models.dtos.WishlistResponseDTO;
import com.quma.quma_shopify_backend.services.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/wishlist")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;

    @PostMapping("/toggle")
    public ResponseEntity<String> toggleWishlist(@RequestBody WishlistRequestDTO request) {
        String result = wishlistService.toggleWishlist(request);
        return ResponseEntity.ok(result);
    }

    @GetMapping
    public ResponseEntity<Page<WishlistResponseDTO>> getWishlist(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(wishlistService.getWishlist(page, size));
    }

    @PostMapping("/bulk-add")
    public ResponseEntity<String> bulkAddToWishlist(@RequestBody List<WishlistRequestDTO> requests) {
        wishlistService.bulkAddToWishlist(requests);
        return ResponseEntity.ok("Bulk wishlist added successfully");
    }
}
