package com.quma.quma_shopify_backend.models.mongo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class Variant {
    @NotBlank
    private String identifier;

    private String color;

    private String size;

    private Double weight;

    private Integer price;

    private Integer discountedPrice;

    private Integer stock;

    private List<String> materials;

    private List<String> images;
}
