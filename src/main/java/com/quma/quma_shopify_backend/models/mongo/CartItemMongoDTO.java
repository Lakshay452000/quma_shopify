package com.quma.quma_shopify_backend.models.mongo;

import java.time.Instant;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.quma.quma_shopify_backend.models.dtos.CartItemDTO;

import lombok.Data;

@Data
@Document(collection = "user_cart")
public class CartItemMongoDTO {

    @Id
    private String id;
    private String username;
    List<CartItemDTO> cartItemDTOs;
    private double totalAmount;
    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();

}
