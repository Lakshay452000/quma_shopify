package com.quma.quma_shopify_backend.models.mongo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Data
@Document(collection = "products")
public class Product {
    @Id
    private String id;

    @NotBlank
    private String title;

    @NotBlank
    private String description;

    @NotBlank
    private String categoryId;

    private List<String> tags;

    @NotBlank
    private List<Variant> variants; // Different sizes, colors, weight

    private Double averageRating;

    private Long totalReviews;

    @NotBlank
    private String ownerId; // The user who created this product

    private Boolean isActive = true; // Whether the product is available for sale

    private List<String> sharedWithUserIds; // Users with edit rights

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    private List<String> images; // Each variant can have its own images
}
