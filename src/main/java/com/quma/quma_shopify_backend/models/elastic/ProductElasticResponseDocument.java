package com.quma.quma_shopify_backend.models.elastic;

import java.util.List;

import lombok.Data;

@Data
public class ProductElasticResponseDocument {
    private List<ProductElasticDocument> products;
    private Object[] sortValues;
}
