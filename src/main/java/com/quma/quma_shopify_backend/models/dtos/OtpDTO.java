package com.quma.quma_shopify_backend.models.dtos;

import com.quma.quma_shopify_backend.enums.OtpPurpose;
import lombok.Data;

@Data
public class OtpDTO {
    private String phone;
    private String otp;
    private boolean isVerified;
    private OtpPurpose otpPurpose;
}
