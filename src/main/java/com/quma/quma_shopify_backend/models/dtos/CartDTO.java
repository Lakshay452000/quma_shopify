package com.quma.quma_shopify_backend.models.dtos;

import lombok.Data;

import java.util.List;

@Data
public class CartDTO {
    private String userId;  // key to store cart in Redis
    private List<CartItemDTO> items;
}

