package com.quma.quma_shopify_backend.models.mongo;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class ProductInventoryRequestDTO {
    private String productId;
    private Integer availableQuantity;
    private BigDecimal price;
    private BigDecimal discountedPrice;
}
