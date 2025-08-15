package com.quma.quma_shopify_backend.models.dtos;

import com.quma.quma_shopify_backend.enums.SortType;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
public class ProductsRequestDTO {
    private List<String> productIds;
    private Map<String, Object> productFilters;
    private int pageSize = 20; // Default page size
    private String lastProductId; // For pagination
    private String sortBy; // Field to sort by, e.g., "price", "rating
    private SortType sortType; // "asc" or "desc"
}
