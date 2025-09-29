package com.quma.quma_shopify_backend.models.mongo;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "productStocks")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductInventoryDTO {
    @Id
    private String id; // combination of productId-identifier
    private String productId;
    private String identifier;
    private int availableQuantity;
    private int price;
}
