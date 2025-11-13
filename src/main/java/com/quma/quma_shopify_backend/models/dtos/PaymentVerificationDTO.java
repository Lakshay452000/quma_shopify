package com.quma.quma_shopify_backend.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentVerificationDTO {
    private String orderId;
    private String razorpayOrderId;
    private String paymentId;
    private String signature;
}
