package com.quma.quma_shopify_backend.models.dtos;

import java.util.List;

import lombok.Data;

@Data
public class CartCheckoutRequestDTO {
    private List<CartItemRequestDTO> items;
}