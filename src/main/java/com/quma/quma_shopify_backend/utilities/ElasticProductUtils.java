package com.quma.quma_shopify_backend.utilities;

import java.util.Arrays;
import java.util.List;

public class ElasticProductUtils {

    // added keyword after tags since it was a text, for keywords you dont use that
    private static final String[] TOP_LEVEL_FILTERS = { "types", "categories", "brand", "color", "size",
            "weight", "materials" };

    public static List<String> getTopLevelFilters() {
        return Arrays.asList(TOP_LEVEL_FILTERS);
    }

    private ElasticProductUtils() {
        // private constructor to prevent instantiation
    }
}
