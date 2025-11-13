package com.quma.quma_shopify_backend.models.mongo;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;

@Document(collection = "reviews")
@CompoundIndex(name = "user_product_idx", def = "{'username': 1, 'productId': 1}", unique = true)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review {

    @Id
    private String id;

    private String name;

    @NotNull
    private String username;

    @NotNull
    private String productId;

    @Min(1)
    @Max(5)
    private int rating;

    @NotBlank
    private String comment;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
