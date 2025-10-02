package com.quma.quma_shopify_backend.controller;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.quma.quma_shopify_backend.models.dtos.CouponApplyResponseDTO;
import com.quma.quma_shopify_backend.models.dtos.CouponCreateDTO;
import com.quma.quma_shopify_backend.models.dtos.CouponDTO;
import com.quma.quma_shopify_backend.services.CouponService;

@RestController
@RequestMapping("/coupons")
public class CouponController {

    @Autowired
    private CouponService couponService;

    // ✅ Apply coupon and get discounted amounts
    @PostMapping("/apply")
    public ResponseEntity<CouponApplyResponseDTO> applyCoupon(
            @RequestParam String code,
            @RequestParam BigDecimal totalAmount) {
        CouponApplyResponseDTO response = couponService.applyCoupon(code, totalAmount);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/create")
    public ResponseEntity<CouponDTO> createCoupon(@RequestBody CouponCreateDTO dto) {
        // String role = UserContext.get().getRole();
        // if (!"ADMIN".equalsIgnoreCase(role)) {
        // throw new ApiException("Unauthorized: Admin access required", 403);
        // }

        CouponDTO coupon = couponService.createCoupon(dto);
        return ResponseEntity.ok(coupon);
    }

}