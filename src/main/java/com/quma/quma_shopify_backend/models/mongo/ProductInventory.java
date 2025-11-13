package com.quma.quma_shopify_backend.models.mongo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "product_inventory")
public class ProductInventory {

    @Id
    private String id;
    private String baseProductId;
    private String productId;
    private BigDecimal price;
    private BigDecimal discountedPrice;
    private Integer availableQuantity;
    private Boolean inStock;
}
