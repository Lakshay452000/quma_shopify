package com.quma.quma_shopify_backend.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

import com.quma.quma_shopify_backend.models.mongo.OrderItemDTO;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderRequestDTO {
    private String userId;
    private List<OrderItemDTO> items;
}
