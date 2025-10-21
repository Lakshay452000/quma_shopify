package com.quma.quma_shopify_backend.models.dtos;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.quma.quma_shopify_backend.enums.SortType;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class ProductsRequestDTO {
    private List<String> productIds;
    private boolean initialLoad;
    private Map<String, List<String>> filters;
    @JsonSetter(nulls = Nulls.SKIP)
    private int pageSize = 20; // Default page size
    private Object[] sortValues;
    @JsonSetter(nulls = Nulls.SKIP)
    private String sortBy = "createdAt";
    @JsonSetter(nulls = Nulls.SKIP)
    private SortType sortType = SortType.DESC; // "asc" or "desc"
    private String searchTerm;
}
