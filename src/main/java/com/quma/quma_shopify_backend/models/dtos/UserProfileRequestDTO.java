package com.quma.quma_shopify_backend.models.dtos;

import com.quma.quma_shopify_backend.enums.Gender;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserProfileRequestDTO {
    @NotBlank(message = "Name is required")
    private String name;
    @NotBlank(message = "Email is required")
    private String email;
    @NotBlank(message = "Gender is required")
    private Gender gender;
    @NotBlank(message = "Date of birth is required")
    private String dateOfBirth;
}
