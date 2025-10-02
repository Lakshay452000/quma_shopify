package com.quma.quma_shopify_backend.models.dtos;

import lombok.Data;

@Data
public class CreateOrderRequestDTO {
    private String couponCode;
    private String addressId;
}
