package com.quma.quma_shopify_backend.models.dtos;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CartResponseDTO {
    private List<CartItemDTO> cartItemDTOs;
    private BigDecimal totalAmount;
    private boolean cartChanged;
}
