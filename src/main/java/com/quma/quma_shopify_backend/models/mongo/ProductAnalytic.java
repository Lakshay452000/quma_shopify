package com.quma.quma_shopify_backend.models.mongo;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Document(collection = "product_analytics")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@CompoundIndex(name = "user_product_identifier_idx", def = "{'username': 1, 'productId': 1, 'identifier': 1, 'date': 1}", unique = true)
public class ProductAnalytic {

    @Id
    private String id;

    private String username;
    private String productId;
    private String identifier;
    private String imageUrl;

    private long views;
    private long favorites;
    private long addedToCart;
    private long orders;
    private long searches;

    private double averageRating;
    private long ratingCount;

    private List<String> categories;

    private Instant lastUpdated;

    @Indexed(name = "date_ttl_idx", expireAfterSeconds = 604800) // 7 days
    private Instant date;
}
