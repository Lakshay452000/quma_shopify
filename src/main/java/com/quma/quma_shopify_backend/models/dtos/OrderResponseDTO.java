package com.quma.quma_shopify_backend.models.dtos;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.quma.quma_shopify_backend.models.mongo.OrderItemDTO;

import lombok.Data;

@Data
public class OrderResponseDTO {
    private String orderId;
    private String username;
    private List<OrderItemDTO> items;
    private BigDecimal amount;
    private String orderPaymentStatus;
    private String orderShipmentStatus;
    private String orderStatus;
    private Instant createdAt;
    private BigDecimal subTotal;
    private BigDecimal discount;
    private BigDecimal shippingFee;
    private ShippingAddressDTO shippingAddress;
}
