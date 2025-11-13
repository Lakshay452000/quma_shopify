package com.quma.quma_shopify_backend.models.mongo;

import java.math.BigDecimal;
import java.util.List;

import lombok.Data;

@Data
public class OrderItemDTO {
    private String productId;
    private String identifier;
    private String title;
    private List<String> description;
    private BigDecimal price;
    private Integer quantity;
    private String color;
    private String size;
    private String imageUrl;
    private List<String> materials;
    private Double weight;
}
