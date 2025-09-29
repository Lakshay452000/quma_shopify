package com.quma.quma_shopify_backend.utilities;

import java.time.Duration;

public class Constants {

        public static final Duration ACCESS_TOKEN_DURATION = Duration.ofMinutes(3);
        public static final Duration REFRESH_TOKEN_DURATION = Duration.ofDays(7);
        public static final int ACCESS_COOKIE_MAX_AGE = (int) ACCESS_TOKEN_DURATION.getSeconds();
        public static final int REFRESH_COOKIE_MAX_AGE = 7 * 24 * 60 * 60;
        public static final String COOKIE_ACCESS_TOKEN_KEY_NAME = "accessToken";
        public static final String COOKIE_REFRESH_TOKEN_KEY_NAME = "refreshToken";
        public static final String COOKIE_ACCESS_TOKEN_ALLOWED_PATH = "/";
        public static final String COOKIE_REFRESH_TOKEN_ALLOWED_PATH = "/auth/new-refresh-token";
        public static final String ELASTIC_PRODUCT_INDEX_NAME = "products";
        public static final String[] ELASTIC_PRODUCT_SEARCH_FIELDS = { "title", "description", "categoryId.text",
                        "type.text",
                        "brand.text", "variants.color.text", "variants.size.text" };
        public static final String[] ELASTIC_PRODUCT_FILTER_FIELDS = { "type", "category", "brand", "color", "size",
                        "material" };

        public static final String REDIS_GLOBAL_FILTERS_KEY = "GLOBAL_FILTERS";
        public static final Integer GUEST_RATE_LIMITOR = 1000;
        public static final Integer AUTH_USER_RATE_LIMITOR = 1000;
}
