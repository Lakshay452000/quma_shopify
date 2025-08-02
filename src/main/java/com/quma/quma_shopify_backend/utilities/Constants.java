package com.quma.quma_shopify_backend.utilities;

import java.time.Duration;

public class Constants {

    public static final Duration ACCESS_TOKEN_DURATION = Duration.ofMinutes(15);
    public static final Duration REFRESH_TOKEN_DURATION = Duration.ofHours(1);
    public static final int ACCESS_COOKIE_MAX_AGE = (int) ACCESS_TOKEN_DURATION.getSeconds();
    public static final int REFRESH_COOKIE_MAX_AGE = 7 * 24 * 60 * 60;
    public static final String COOKIE_ACCESS_TOKEN_KEY_NAME = "accessToken";
    public static final String COOKIE_REFRESH_TOKEN_KEY_NAME = "refreshToken";
    public static final String COOKIE_ACCESS_TOKEN_ALLOWED_PATH = "/";
    public static final String COOKIE_REFRESH_TOKEN_ALLOWED_PATH = "/auth/new-refresh-token";
    public static final String REDIS_OTP_KEY_PREFIX = "otp_";
    public static final String REDIS_RESET_PASSWORD_KEY_PREFIX = "reset_password_";
}
