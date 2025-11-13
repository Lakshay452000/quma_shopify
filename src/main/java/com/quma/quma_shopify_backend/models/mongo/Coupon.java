package com.quma.quma_shopify_backend.models.mongo;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Data
@Document(collection = "coupons")
public class Coupon {
    @Id
    private String id;

    private String code;
    private BigDecimal discountAmount;
    private Double discountPercent;
    private Boolean active;
    private Instant validFrom;
    private Instant validUntil;

    private Boolean oneTimeUse; // true if user can use only once
    private List<String> eligibleUsers; // null or empty = all users
}
