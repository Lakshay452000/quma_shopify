package com.quma.quma_shopify_backend.models.dtos;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CartItemDTO {
    private String productId; // productId for frontend
    private String identifier; // variant identifier
    private String title;
    private List<String> description;

    private BigDecimal price;
    private BigDecimal discountedPrice; // nullable if no discount

    private String color;
    private String size;
    private String imageUrl;
    private Integer stock;
    private List<String> materials;
    private Double weight;

    private int quantity;
}
