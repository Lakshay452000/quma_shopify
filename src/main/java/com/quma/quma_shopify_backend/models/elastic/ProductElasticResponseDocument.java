package com.quma.quma_shopify_backend.models.elastic;

import java.util.List;
import java.util.Map;

import lombok.Data;

@Data
public class ProductElasticResponseDocument {
    private List<ProductElasticDocument> products;
    private Object[] sortValues;
    private Map<String, List<String>> filters;
    private List<String> quickFilters;
}
