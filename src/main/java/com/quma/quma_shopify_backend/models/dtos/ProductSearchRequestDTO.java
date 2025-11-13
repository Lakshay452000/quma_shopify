package com.quma.quma_shopify_backend.models.dtos;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.quma.quma_shopify_backend.enums.SortType;

import lombok.Data;

@Data
public class ProductSearchRequestDTO {
    private List<String> productIds;
    private boolean initialLoad;
    private Map<String, List<String>> filters;
    @JsonSetter(nulls = Nulls.SKIP)
    private int pageSize = 20;
    private Object[] sortValues;
    @JsonSetter(nulls = Nulls.SKIP)
    private String sortBy = "createdAt";
    @JsonSetter(nulls = Nulls.SKIP)
    private SortType sortType = SortType.DESC;
    private String searchTerm;
}
