package com.quma.quma_shopify_backend.repositories.mongo;

public interface ProductRepositoryCustom {
    void updateVariantWishlisted(String productId, String variantIdentifier, boolean isWishlisted);
}
