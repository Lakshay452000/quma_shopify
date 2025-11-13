package com.quma.quma_shopify_backend.models.dtos;

import lombok.Data;

@Data
public class CartItemRequestDTO {
    private String productId; // productId for frontend
    private String identifier; // variant identifier
    private int quantity;
}
