package com.quma.quma_shopify_backend.models.dtos;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class RazorpayPaymentDTO {
    private String id;
    private String status;
    private String orderId;
    private long amount;
    private String method;
}
