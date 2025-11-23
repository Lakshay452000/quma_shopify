package com.quma.quma_shopify_backend.models.dtos;

import lombok.Data;

@Data
public class PaymentRequestDTO {
    private String orderId;
    private String addressId;
    private String couponCode;
}
