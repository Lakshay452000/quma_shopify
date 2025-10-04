package com.quma.quma_shopify_backend.models.mongo;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "wishlists")
public class Wishlist {

    @Id
    private String id;
    private String username;
    private String productId;
    private String identifier; // variant identifier

    // Product info needed for frontend ProductCard
    private String title;
    private BigDecimal price;
    private String imageUrl;
    private String brand;
    private Double rating;
    private Long reviews;
    private String color;
    private String size;

    private Instant createdAt;
}
