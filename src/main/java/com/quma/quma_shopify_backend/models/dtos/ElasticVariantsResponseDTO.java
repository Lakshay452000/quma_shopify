package com.quma.quma_shopify_backend.models.dtos;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class ElasticVariantsResponseDTO {

    private String baseProductId;
    private String productId;
    private BigDecimal price;
    private BigDecimal discountedPrice;
    private String color;
    private String image;
}
