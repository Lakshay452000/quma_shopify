package com.quma.quma_shopify_backend.services;

import com.quma.quma_shopify_backend.interfaces.IStore;
import com.quma.quma_shopify_backend.security.JwtUtil;
import com.quma.quma_shopify_backend.utilities.Constants;
import com.quma.quma_shopify_backend.utilities.CookieUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class AuthService {

    @Value("${imagekit.privateKey}")
    private String privateKey;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private IStore tokenStore;

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

    public Map<String, Object> getCdnUploadAuthSignature() {
        Map<String, Object> response = new HashMap<>();
        try {
            String token = UUID.randomUUID().toString();
            long expire = (System.currentTimeMillis() / 1000) + 3600; // 3600 seconds = 1 hour
            String dataToSign = token + expire;
            String signature = generateHmacSha1(dataToSign, privateKey);
            response.put("token", token);
            response.put("expire", expire);
            response.put("signature", signature);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            System.err.println("Error generating signature: " + e.getMessage());
            response.put("error", "Failed to generate authentication parameters.");
        }
        return response;
    }

    private String generateHmacSha1(String data, String key)
            throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance("HmacSHA1");
        SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA1");
        mac.init(secretKeySpec);
        byte[] hmacSha1Bytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder hexString = new StringBuilder();
        for (byte b : hmacSha1Bytes) {
            hexString.append(String.format("%02x", b));
        }
        return hexString.toString();
    }
}