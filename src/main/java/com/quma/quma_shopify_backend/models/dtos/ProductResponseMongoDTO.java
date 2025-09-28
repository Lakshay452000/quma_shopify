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

    private List<Variant> variants;

    private Double averageRating;

    private Long totalReviews;

    private String ownerId; // The user who created this product

    private Boolean isActive = true; // Whether the product is available for sale

    private Instant createdAt;

    private Instant updatedAt;

}