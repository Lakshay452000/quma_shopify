package com.quma.quma_shopify_backend.models.mongo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class Variant {
    @NotBlank
    private String identifier; // Unique identifier per variant

    private String color;

    private String size; // Small, Medium, Large

    private Double weight; // in kg

    private BigDecimal price; // Base price, can be overridden by variants

    private BigDecimal discountedPrice; // Base price, can be overridden by variants

    private Integer stock;
}
