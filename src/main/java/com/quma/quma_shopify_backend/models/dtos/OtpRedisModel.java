package com.quma.quma_shopify_backend.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OtpRedisModel implements Serializable {
    private static final long serialVersionUID = 1L;
    private String phone;
    private String otp;
    private boolean verified;
}