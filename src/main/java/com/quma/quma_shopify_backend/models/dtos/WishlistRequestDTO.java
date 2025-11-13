package com.quma.quma_shopify_backend.models.dtos;

import lombok.Data;

@Data
public class WishlistRequestDTO {
    private String productId;
    private String identifier;
    private boolean markWishlist;
}
