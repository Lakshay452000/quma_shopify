package com.quma.quma_shopify_backend.utilities;

public class ElasticProductFilters {

    private static final String[] TOP_LEVEL_FILTERS = { "type", "category", "brand" };
    private static final String[] NESTED_FILTERS = { "color", "size", "material" };

    public static String[] getTopLevelFilters() {
        return TOP_LEVEL_FILTERS;
    }

    public static String[] getNestedFilters() {
        return NESTED_FILTERS;
    }

    private ElasticProductFilters() {
        // private constructor to prevent instantiation
    }
}
