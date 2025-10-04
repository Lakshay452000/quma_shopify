package com.quma.quma_shopify_backend.models.dtos;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WishlistResponseDTO {
    private String productId;
    private String identifier;
    private String title;
    private BigDecimal price;
    private String imageUrl;
    private String brand;
    private Double rating;
    private Integer buyers;
    private String color;
    private String size;
}
