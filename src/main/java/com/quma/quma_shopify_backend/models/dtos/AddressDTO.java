package com.quma.quma_shopify_backend.models.dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AddressDTO {

    private String addressId;
    private String name;
    private Long phoneNumber;
    private String username;
    private String house;
    private String area;
    private String city;
    private String state;
    @NotBlank
    private String pincode;
    private boolean isDefault;
}
