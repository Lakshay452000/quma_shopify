package com.quma.quma_shopify_backend.models.dtos;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CartResponseDTO {
    private List<CartItemDTO> cartItemDTOs;
    private double totalAmount;
    private boolean cartChanged;
}
