package com.quma.quma_shopify_backend.models.dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserRequestDTO {
    private String name;
    @NotBlank(message = "Phone number is required")
    private String phone;
    private String email;
    @NotBlank(message = "Password is required")
    private String password;
    private String macAddress;
    private String address;
    private String city;
    private String state;
    private String country;
    private boolean resetPassword;
}

