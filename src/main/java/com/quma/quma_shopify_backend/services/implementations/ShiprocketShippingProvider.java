package com.quma.quma_shopify_backend.services.implementations;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

import com.quma.quma_shopify_backend.interfaces.IShippingProvider;

@Service
public class ShiprocketShippingProvider implements IShippingProvider {

    @Override
    public String getName() {
        return "Shiprocket";
    }

    @Override
    public BigDecimal getShippingCharge(String pincode, double weight) {
        // call Shiprocket API here
        // extract and return charge
        return new BigDecimal("50"); // example
    }
}
