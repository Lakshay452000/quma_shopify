package com.quma.quma_shopify_backend.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class VerifyPaymentDTO {
    private String orderId; // internal order id
    private String razorpayOrderId;
    private String paymentId;
    private String signature;
}
