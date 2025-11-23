package com.quma.quma_shopify_backend.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PaymentDetailsRequestDTO {
    private String orderId;
    private String razorpayOrderId;
    private String paymentId;
    private String signature;
}
