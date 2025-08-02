package com.quma.quma_shopify_backend.services;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimiterService {

    private final Map<String, Bucket> ipBucketCache = new ConcurrentHashMap<>();
    private final Map<String, Bucket> userBucketCache = new ConcurrentHashMap<>();

    private static final Bandwidth ipLimit = Bandwidth.classic(5, Refill.intervally(5, Duration.ofMinutes(1))); // 5 req/min
    private static final Bandwidth userLimit = Bandwidth.classic(20, Refill.intervally(20, Duration.ofMinutes(1))); // 20 req/min

    public boolean isAllowed(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            String ip = request.getRemoteAddr();
            return getBucket(ipBucketCache, ip, ipLimit).tryConsume(1);
        } else {
            String user = auth.getName();
            return getBucket(userBucketCache, user, userLimit).tryConsume(1);
        }
    }

    private Bucket getBucket(Map<String, Bucket> cache, String key, Bandwidth limit) {
        return cache.computeIfAbsent(key, k -> Bucket.builder().addLimit(limit).build());
    }
}
