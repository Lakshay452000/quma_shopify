package com.quma.quma_shopify_backend.repositories.mongo;

import com.quma.quma_shopify_backend.models.mongo.Review;

import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import com.quma.quma_shopify_backend.interfaces.IProductRatingAggregate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.List;

public interface ReviewRepository extends MongoRepository<Review, String> {
    Optional<Review> findByUsernameAndProductId(String username, String productId);

    List<Review> findAllByProductId(String productId);

    @Aggregation(pipeline = {
            "{ $match: { productId: ?0 } }",
            "{ $group: { _id: null, avgRating: { $avg: '$rating' }, count: { $sum: 1 } } }"
    })
    IProductRatingAggregate calculateRatingSummary(String productId);

    Page<Review> findByProductId(String productId, Pageable pageable);

}
