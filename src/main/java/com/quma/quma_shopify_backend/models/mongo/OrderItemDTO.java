package com.quma.quma_shopify_backend.models.mongo;

import lombok.Data;

@Data
public class OrderItemDTO {
    private String productId;
    private int quantity;
    private long price;
}
