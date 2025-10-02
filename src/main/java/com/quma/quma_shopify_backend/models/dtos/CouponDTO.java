package com.quma.quma_shopify_backend.models.dtos;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import lombok.Data;

@Data
public class CouponDTO {
    private String code;
    private BigDecimal discountAmount;
    private Double discountPercent;
    private Boolean active;
    private Instant validFrom;
    private Instant validUntil;

    private Boolean oneTimeUse; // true if user can use only once
    private List<String> eligibleUsers; // null or empty = all users
}
