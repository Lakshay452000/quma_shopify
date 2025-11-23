package com.quma.quma_shopify_backend.enums;

public enum OrderPaymentStatus {
    // For PENDING: always when order is created or cron changes its status after
    // expiry
    PENDING, PROCESSING, PAID, FAILED, CANCELLED, REFUNDED
}
