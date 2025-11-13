package com.quma.quma_shopify_backend.models.elastic;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import lombok.Data;

@Data
public class ProductElasticDocumentDTO {

    private String productId;
    private String identifier;
    private String title;
    private String brand;
    private List<String> categories;
    private List<String> images;
    private BigDecimal price;
    private BigDecimal discountedPrice;
    private Double averageRating;
    private Long totalBuyers;
    private Boolean isActive;
    private Instant createdAt;
    private Instant updatedAt;
    private boolean wishlisted = false;

}
