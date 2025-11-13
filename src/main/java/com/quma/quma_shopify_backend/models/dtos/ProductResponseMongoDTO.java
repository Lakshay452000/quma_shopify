package com.quma.quma_shopify_backend.models.dtos;

import java.time.Instant;
import java.util.List;

import com.quma.quma_shopify_backend.models.mongo.Variant;

import lombok.Data;

@Data
public class ProductResponseMongoDTO {
    private String productId;
    private String title;
    private List<String> description;
    private String brand;
    private List<Variant> variants; // Different sizes, colors, weight
    private Double averageRating;
    private Long totalReviews;
    private Long totalBuyers;
    private Boolean isActive = true;
    private List<String> sharedWithUserIds;
    private Instant createdAt;
    private Instant updatedAt;
    private boolean wishlisted;
}
