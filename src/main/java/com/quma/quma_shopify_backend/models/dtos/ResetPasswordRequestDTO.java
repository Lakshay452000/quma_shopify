package com.quma.quma_shopify_backend.models.dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ResetPasswordRequestDTO {
    @NotBlank(message = "Phone number is required")
    private String phone;
    @NotBlank(message = "New password is required")
    private String newPassword;
}
