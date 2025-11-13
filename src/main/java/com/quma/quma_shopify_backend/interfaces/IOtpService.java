package com.quma.quma_shopify_backend.interfaces;

import com.quma.quma_shopify_backend.enums.OtpPurpose;
import com.quma.quma_shopify_backend.models.dtos.OtpDTO;

public interface IOtpService {

    void ensureOtpVerified(String phone, OtpPurpose otpPurpose);

    void sendOtp(OtpDTO otpDTO);

    void verifyOtp(OtpDTO otpDTO);
}
