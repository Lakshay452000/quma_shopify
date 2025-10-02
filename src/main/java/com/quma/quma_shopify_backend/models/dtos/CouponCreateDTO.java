package com.quma.quma_shopify_backend.models.dtos;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import lombok.Data;

@Data
public class CouponCreateDTO {
    private String code; // unique coupon code
    private Boolean active = true;
    private Instant validFrom;
    private Instant validUntil;
    private BigDecimal discountAmount;
    private Double discountPercent;
    private Boolean oneTimeUse = true; // can only be used once per user
    private List<String> eligibleUsers; // null or empty means all users
}
