package com.quma.quma_shopify_backend.models.dtos;

import com.quma.quma_shopify_backend.models.mongo.Order;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderResponseDTO {
    private Order order;
    private String paymentUrl;
}