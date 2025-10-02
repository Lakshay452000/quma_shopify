package com.quma.quma_shopify_backend.controller;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.quma.quma_shopify_backend.services.ShippingService;

@RestController
@RequestMapping("/shipping")
public class ShippingController {

    @Autowired
    ShippingService shippingService;

    @GetMapping("/charge")
    public ResponseEntity<BigDecimal> getShippingCharge(
            @RequestParam String provider,
            @RequestParam String addressId) {

        BigDecimal charge = shippingService.calculateShipping(provider,
                addressId);
        return ResponseEntity.ok(charge);
    }

    // @GetMapping("/options")
    // public ResponseEntity<List<ShippingOptionDTO>> getShippingOptions(
    // @RequestParam String cartId,
    // @RequestParam String addressId) {

    // List<ShippingOptionDTO> options = shippingService.getAvailableOptions(cartId,
    // addressId);
    // return ResponseEntity.ok(options);
    // }
}
