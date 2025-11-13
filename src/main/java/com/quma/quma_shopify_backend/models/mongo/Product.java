package com.quma.quma_shopify_backend.models.mongo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
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

    private String productId;

    @NotBlank
    private String title;

    @NotEmpty
    private List<String> description;

    @NotBlank
    private String brand;

    @NotEmpty
    private List<String> categories;

    @NotEmpty
    private List<String> types;

    private List<String> tags;

    @NotEmpty
    private List<Variant> variants; // Different sizes, colors, weight

    private Double averageRating;

    private Long totalReviews;

    private Long totalBuyers;

    @NotBlank
    private String ownerId; // The user who created this product

    private Boolean isActive = true; // Whether the product is available for sale

    private List<String> sharedWithUserIds; // Users with edit rights

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    @NotBlank
    private String productAddress;
}
