package com.quma.quma_shopify_backend.models.dtos;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class CouponApplyResponseDTO {
    private BigDecimal originalAmount;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;
    private String couponCode;
}
