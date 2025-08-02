package com.quma.quma_shopify_backend.services;

import com.quma.quma_shopify_backend.interfaces.ITokenStore;
import com.quma.quma_shopify_backend.security.JwtUtil;
import com.quma.quma_shopify_backend.utilities.Constants;
import com.quma.quma_shopify_backend.utilities.CookieUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    @Value("${frontend.redirect.home-url}")
    private String frontendRedirectUrl;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private ITokenStore tokenStore;


    private static void setAccessAndRefreshCookie(HttpServletResponse response, String accessToken, String refreshToken) {
        // Setting access token cookie
        CookieUtil.setCookie(response, Constants.COOKIE_ACCESS_TOKEN_KEY_NAME, accessToken, Constants.ACCESS_COOKIE_MAX_AGE, Constants.COOKIE_ACCESS_TOKEN_ALLOWED_PATH);
        // Setting refresh token cookie
        CookieUtil.setCookie(response, Constants.COOKIE_REFRESH_TOKEN_KEY_NAME, refreshToken, Constants.REFRESH_COOKIE_MAX_AGE, Constants.COOKIE_REFRESH_TOKEN_ALLOWED_PATH);
    }

    private static void clearAccessAndRefreshCookie(HttpServletResponse response) {
        CookieUtil.clearCookie(response, Constants.COOKIE_ACCESS_TOKEN_KEY_NAME, Constants.COOKIE_ACCESS_TOKEN_ALLOWED_PATH);
        CookieUtil.clearCookie(response, Constants.COOKIE_REFRESH_TOKEN_KEY_NAME, Constants.COOKIE_REFRESH_TOKEN_ALLOWED_PATH);
    }

    public void login(String username, HttpServletResponse response) {
        String accessToken = jwtUtil.generateToken(username, Constants.ACCESS_TOKEN_DURATION.toMillis());
        String refreshToken = jwtUtil.generateToken(username, Constants.REFRESH_TOKEN_DURATION.toMillis());

        tokenStore.save(refreshToken, username, Constants.REFRESH_TOKEN_DURATION);
        setAccessAndRefreshCookie(response, accessToken, refreshToken);
    }

    public void logout(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = extractRefreshTokenFromCookie(request);
        if (refreshToken != null) {
            tokenStore.delete(refreshToken);
        }
        clearAccessAndRefreshCookie(response);
    }

    public String extractAccessTokenFromCookie(HttpServletRequest request) {
        return CookieUtil.getCookieValue(request, Constants.COOKIE_ACCESS_TOKEN_KEY_NAME);
    }

    public String extractRefreshTokenFromCookie(HttpServletRequest request) {
        return CookieUtil.getCookieValue(request, Constants.COOKIE_REFRESH_TOKEN_KEY_NAME);
    }

    public String getUsernameFromRefreshToken(String token) {
        return tokenStore.get(token, String.class);
    }

    public void replaceRefreshToken(String oldToken, String newToken, String username) {
        tokenStore.delete(oldToken);
        tokenStore.save(newToken, username, Constants.REFRESH_TOKEN_DURATION);
    }

    public boolean refreshToken(HttpServletRequest request, HttpServletResponse response) {
        String oldRefreshToken = extractRefreshTokenFromCookie(request);
        if (oldRefreshToken == null || !jwtUtil.isValid(oldRefreshToken)) {
            return false;
        }

        String username = getUsernameFromRefreshToken(oldRefreshToken);
        if (username == null) {
            return false;
        }

        String newAccessToken = jwtUtil.generateToken(username, Constants.ACCESS_TOKEN_DURATION.toMillis());
        String newRefreshToken = jwtUtil.generateToken(username, Constants.REFRESH_TOKEN_DURATION.toMillis());

        replaceRefreshToken(oldRefreshToken, newRefreshToken, username); // Save new and delete old
        setAccessAndRefreshCookie(response, newAccessToken, newRefreshToken);

        return true;
    }

    public boolean authStatus(HttpServletRequest request) {
        String token = CookieUtil.getCookieValue(request, Constants.COOKIE_ACCESS_TOKEN_KEY_NAME);
        return token != null && jwtUtil.isValid(token);
    }
}