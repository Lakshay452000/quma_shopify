package com.quma.quma_shopify_backend.models.dtos;

import com.quma.quma_shopify_backend.enums.OrderPaymentStatus;
import com.quma.quma_shopify_backend.enums.OrderShipmentStatus;
import com.quma.quma_shopify_backend.enums.OrderStatus;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class OrderResponseDTO {
    private String orderId;
    private Long amount;
    private OrderPaymentStatus orderPaymentStatus;
    private OrderShipmentStatus orderShipmentStatus;
    private OrderStatus orderStatus;
}
