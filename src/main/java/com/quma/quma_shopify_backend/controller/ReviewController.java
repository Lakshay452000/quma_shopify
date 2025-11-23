package com.quma.quma_shopify_backend.controller;

import com.quma.quma_shopify_backend.models.dtos.ReviewRequestDTO;
import com.quma.quma_shopify_backend.models.mongo.Review;
import com.quma.quma_shopify_backend.services.ReviewService;
import com.quma.quma_shopify_backend.models.dtos.ReviewResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping("/save")
    public ResponseEntity<Review> addOrUpdateReview(
            @Valid @RequestBody ReviewRequestDTO request) {
        Review review = reviewService.addOrUpdateReview(request);
        return ResponseEntity.ok(review);
    }

    @GetMapping("/{productId}")
    public ResponseEntity<Page<ReviewResponseDTO>> getReviews(
            @PathVariable String productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        Sort.Direction sortDirection = direction.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        PageRequest pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));

        Page<ReviewResponseDTO> reviews = reviewService.getProductReviews(productId, pageable);
        return ResponseEntity.ok(reviews);
    }
}
