package com.quma.quma_shopify_backend.repositories.mongo;

public interface OrderRepositoryCustom {
    long expireOrders();

    long resetProcessingOrders();
}
