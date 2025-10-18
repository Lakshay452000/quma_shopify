package com.quma.quma_shopify_backend.services;

import com.quma.quma_shopify_backend.models.mongo.Review;
import com.quma.quma_shopify_backend.repositories.mongo.ReviewRepository;
import com.quma.quma_shopify_backend.services.implementations.UserService;
import com.quma.quma_shopify_backend.repositories.mongo.ProductRepository;
import com.quma.quma_shopify_backend.utilities.UserContext;
import com.quma.quma_shopify_backend.models.dtos.ReviewRequestDTO;
import com.quma.quma_shopify_backend.interfaces.IProductRatingAggregate;
import com.quma.quma_shopify_backend.exceptions.ApiException;
import com.quma.quma_shopify_backend.models.dtos.ReviewResponseDTO;
import com.quma.quma_shopify_backend.utilities.Constants;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final OrderService orderService;
    private final ElasticSearchService elasticSearchService;
    private final UserService userService;

    public Review addOrUpdateReview(ReviewRequestDTO request) {
        String username = UserContext.get().getUsername();
        boolean hasPurchased = orderService.hasUserPurchasedProduct(username, request.productId());
        if (!hasPurchased) {
            throw new ApiException("User has not purchased this product", 400);
        }

        Review review = reviewRepository.findByUsernameAndProductId(username, request.productId())
                .map(existing -> {
                    existing.setRating(request.rating());
                    existing.setComment(request.comment());
                    existing.setUpdatedAt(LocalDateTime.now());
                    return reviewRepository.save(existing);
                })
                .orElseGet(() -> {
                    Map<String, Object> userProfile = userService.getUserProfile();
                    Review newReview = Review.builder()
                            .username(username)
                            .name(userProfile.get("name").toString())
                            .productId(request.productId())
                            .rating(request.rating())
                            .comment(request.comment())
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .build();
                    return reviewRepository.save(newReview);
                });

        updateProductRating(request.productId());
        return review;
    }

    private void updateProductRating(String productId) {
        IProductRatingAggregate stats = reviewRepository.calculateRatingSummary(productId);
        if (stats == null)
            return;

        productRepository.findByProductId(productId).ifPresent(product -> {
            double avgRating = Math.round(stats.getAvgRating() * 10.0) / 10.0;
            long totalReviews = stats.getCount();

            // --- Update MongoDB product ---
            product.setAverageRating(avgRating);
            product.setTotalReviews(totalReviews);
            productRepository.save(product);

            // --- Update Elasticsearch product using generalized method ---
            elasticSearchService.updateFields(
                    Constants.ELASTIC_PRODUCT_INDEX_NAME,
                    product.getId(),
                    Map.of(
                            "averageRating", avgRating,
                            "totalReviews", totalReviews));
        });
    }

    public Page<ReviewResponseDTO> getProductReviews(String productId, Pageable pageable) {
        return reviewRepository.findByProductId(productId, pageable)
                .map(review -> new ReviewResponseDTO(
                        review.getName(),
                        review.getUsername(),
                        review.getRating(),
                        review.getComment(),
                        review.getCreatedAt(),
                        review.getUpdatedAt()));
    }
}
