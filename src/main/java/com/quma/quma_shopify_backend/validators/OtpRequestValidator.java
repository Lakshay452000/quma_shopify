package com.quma.quma_shopify_backend.validators;

import com.quma.quma_shopify_backend.models.dtos.OtpDTO;
import org.springframework.stereotype.Component;

@Component
public class OtpRequestValidator {
    public void validateOtpRequest(OtpDTO otpDTO) {
        if (otpDTO == null) {
            throw new IllegalArgumentException("OTP request cannot be null");
        }
        if (otpDTO.getPhone() == null || otpDTO.getPhone().isEmpty()) {
            throw new IllegalArgumentException("Phone number is required");
        }
        if (otpDTO.getOtpPurpose() == null) {
            throw new IllegalArgumentException("OTP Purpose is required");
        }
    }
}
