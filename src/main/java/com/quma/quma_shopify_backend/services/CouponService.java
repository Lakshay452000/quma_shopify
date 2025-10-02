package com.quma.quma_shopify_backend.services;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quma.quma_shopify_backend.exceptions.ApiException;
import com.quma.quma_shopify_backend.models.dtos.CouponApplyResponseDTO;
import com.quma.quma_shopify_backend.models.dtos.CouponCreateDTO;
import com.quma.quma_shopify_backend.models.dtos.CouponDTO;
import com.quma.quma_shopify_backend.models.mongo.Coupon;
import com.quma.quma_shopify_backend.models.mongo.CouponUsage;
import com.quma.quma_shopify_backend.repositories.mongo.CouponRepository;
import com.quma.quma_shopify_backend.repositories.mongo.CouponUsageRepository;
import com.quma.quma_shopify_backend.utilities.UserContext;

@Service
public class CouponService {

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private CouponUsageRepository usageRepository; // Mongo-backed now

    @Autowired
    private ObjectMapper objectMapper;

    private CouponDTO validateCoupon(String code) {
        CouponDTO coupon = objectMapper.convertValue(
                couponRepository.findByCode(code)
                        .orElseThrow(() -> new ApiException("Invalid coupon code", 400)),
                CouponDTO.class);

        String username = UserContext.get().getUsername();
        if (username == null)
            throw new ApiException("Unauthenticated user", 401);

        Instant now = Instant.now();
        if ((coupon.getValidFrom() != null && now.isBefore(coupon.getValidFrom())) ||
                (coupon.getValidUntil() != null && now.isAfter(coupon.getValidUntil())) ||
                !coupon.getActive()) {
            throw new ApiException("Coupon expired or inactive", 400);
        }

        if (coupon.getEligibleUsers() != null && !coupon.getEligibleUsers().isEmpty() &&
                !coupon.getEligibleUsers().contains(username)) {
            throw new ApiException("Coupon not eligible for this user", 400);
        }

        // ✅ Check in Mongo if this user already used this coupon
        if (Boolean.TRUE.equals(coupon.getOneTimeUse()) &&
                usageRepository.existsByCouponCodeAndUsername(code, username)) {
            throw new ApiException("Coupon already used by this user", 400);
        }

        return coupon;
    }

    public CouponApplyResponseDTO applyCoupon(String code, BigDecimal totalAmount) {
        CouponDTO coupon = validateCoupon(code);

        BigDecimal discount = BigDecimal.ZERO;
        BigDecimal finalAmount = totalAmount;

        if (coupon.getDiscountPercent() != null) {
            discount = totalAmount.multiply(BigDecimal.valueOf(coupon.getDiscountPercent() / 100));
        } else if (coupon.getDiscountAmount() != null) {
            discount = coupon.getDiscountAmount();
        }

        finalAmount = totalAmount.subtract(discount);
        if (finalAmount.compareTo(BigDecimal.ZERO) < 0)
            finalAmount = BigDecimal.ZERO;

        if (Boolean.TRUE.equals(coupon.getOneTimeUse())) {
            String username = UserContext.get().getUsername();
            CouponUsage usage = new CouponUsage();
            usage.setCouponCode(code);
            usage.setUsername(username);
            usage.setUsedAt(Instant.now());
            usageRepository.save(usage);
        }

        CouponApplyResponseDTO response = new CouponApplyResponseDTO();
        response.setOriginalAmount(totalAmount);
        response.setDiscountAmount(discount);
        response.setFinalAmount(finalAmount);
        response.setCouponCode(code);

        return response;
    }

    public CouponDTO createCoupon(CouponCreateDTO dto) {
        // Check if code already exists
        if (couponRepository.findByCode(dto.getCode()).isPresent()) {
            throw new ApiException("Coupon code already exists", 400);
        }

        Coupon coupon = new Coupon();
        coupon.setCode(dto.getCode());
        coupon.setActive(dto.getActive());
        coupon.setValidFrom(dto.getValidFrom());
        coupon.setValidUntil(dto.getValidUntil());
        coupon.setDiscountAmount(dto.getDiscountAmount());
        coupon.setDiscountPercent(dto.getDiscountPercent());
        coupon.setOneTimeUse(dto.getOneTimeUse());
        coupon.setEligibleUsers(dto.getEligibleUsers());

        coupon = couponRepository.save(coupon);

        return objectMapper.convertValue(coupon, CouponDTO.class);
    }

}
