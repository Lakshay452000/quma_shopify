package com.quma.quma_shopify_backend.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InitiatePaymentResponseDTO {
    private String key; // Razorpay keyId
    private String orderId; // internal orderId
    private String razorpayOrderId; // Razorpay order id
    private long amount; // amount in paise
    private String currency;
    private String receipt;
    private String status;
}