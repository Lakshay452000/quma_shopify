package com.quma.quma_shopify_backend.utilities;

import java.util.Arrays;
import java.util.List;

public class ElasticProductUtils {

    // added keyword after tags since it was a text, for keywords you dont use that
    private static final String[] TOP_LEVEL_FILTERS = { "types", "categories", "brand", "tags.keyword" };
    private static final String[] NESTED_FILTERS = { "color", "size", "materials" };
    // add variants.color like this for nested fields
    private static final String[] SEARCH_SUGGESTED_FIELDS = { "types", "categories", "brand", "tags" };

    public static List<String> getTopLevelFilters() {
        return Arrays.asList(TOP_LEVEL_FILTERS);
    }

    public static List<String> getNestedFilters() {
        return Arrays.asList(NESTED_FILTERS);
    }

    public static String[] getSearchSuggestedFields() {
        return SEARCH_SUGGESTED_FIELDS;
    }

    private ElasticProductUtils() {
        // private constructor to prevent instantiation
    }
}
