package com.quma.quma_shopify_backend.controller;

import com.quma.quma_shopify_backend.models.dtos.WishlistRequestDTO;
import com.quma.quma_shopify_backend.models.dtos.WishlistResponseDTO;
import com.quma.quma_shopify_backend.services.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/wishlist")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;

    @PostMapping("/toggle")
    public ResponseEntity<String> toggleWishlist(@RequestBody WishlistRequestDTO request) throws Exception {
        String result = wishlistService.toggleWishlist(request);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/")
    public ResponseEntity<List<WishlistResponseDTO>> getWishlist() throws Exception {
        return ResponseEntity.ok(wishlistService.getWishlist());
    }

    @PostMapping("/bulk-add")
    public ResponseEntity<String> bulkAddToWishlist(@RequestBody List<WishlistRequestDTO> requests) {
        wishlistService.bulkAddToWishlist(requests);
        return ResponseEntity.ok("Bulk wishlist added successfully");
    }
}
