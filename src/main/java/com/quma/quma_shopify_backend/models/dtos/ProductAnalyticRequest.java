package com.quma.quma_shopify_backend.models.dtos;

import com.quma.quma_shopify_backend.enums.AnalyticsEventType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductAnalyticRequest {

    @NotBlank(message = "Product ID cannot be blank")
    private String productId;

    private String username;

    private String imageUrl;

    @NotBlank(message = "Identifier cannot be blank")
    private String identifier;

    @NotNull(message = "Event type is required")
    private AnalyticsEventType eventType;

    private Double ratingValue;

    @Size(max = 10, message = "A product can have up to 10 categories")
    private List<@NotBlank(message = "Category name cannot be blank") String> categories;
}
