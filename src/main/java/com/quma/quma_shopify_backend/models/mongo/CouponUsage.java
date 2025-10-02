package com.quma.quma_shopify_backend.models.mongo;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

import java.time.Instant;

@Data
@Document(collection = "coupon_usage")
public class CouponUsage {
    @Id
    private String id;

    private String couponCode;
    private String username;
    private Instant usedAt;
}
