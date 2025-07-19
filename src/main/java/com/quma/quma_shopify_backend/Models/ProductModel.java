package com.quma.quma_shopify_backend.Models;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "products")
public class ProductModel {
    @Id
    String id;
    String title;
}
