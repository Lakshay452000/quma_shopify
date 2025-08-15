package com.quma.quma_shopify_backend.models.dtos;

import lombok.Data;

@Data
public class CartItemDTO {
    private String productId;
    private String productName;
    private String imageUrl;    // needed when displaying cart items to user
    private int quantity;       // needed when displaying quantity of each item in cart
}

