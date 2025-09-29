package com.quma.quma_shopify_backend.models.mongo;

import lombok.Data;

@Data
public class ProductInventoryRequestDTO {
    private String productId;
    private String identifier;
    private int availableQuantity;
    private int price;
}
