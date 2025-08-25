package com.quma.quma_shopify_backend.utilities;

public class ElasticProductFilters {

    // added keyword after tags since it was a text, for keywords you dont use that
    private static final String[] TOP_LEVEL_FILTERS = { "type", "category", "brand", "tags.keyword" };
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
