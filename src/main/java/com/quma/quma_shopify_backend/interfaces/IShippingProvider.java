package com.quma.quma_shopify_backend.interfaces;

import java.math.BigDecimal;

public interface IShippingProvider {
    String getName();

    BigDecimal getShippingCharge(String pincode, double weight);
}
