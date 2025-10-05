package com.quma.quma_shopify_backend.models.dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserRequestDTO {
    @NotBlank(message = "Phone number is required")
    private String phone;
    @NotBlank(message = "Password is required")
    private String password;

}
