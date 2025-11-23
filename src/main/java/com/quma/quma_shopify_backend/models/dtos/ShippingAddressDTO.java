package com.quma.quma_shopify_backend.models.dtos;

import lombok.Data;

@Data
public class ShippingAddressDTO {

    private String addressId;
    private String name;
    private Long phoneNumber;
    private String house;
    private String area;
    private String city;
    private String state;
    private String pincode;
}
