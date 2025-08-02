package com.quma.quma_shopify_backend.services.implementations;

import com.quma.quma_shopify_backend.interfaces.ITokenStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Service
public class RedisStore implements ITokenStore {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public <T> void save(String token, T value, Duration duration) {
        redisTemplate.opsForValue().set(token, value, duration);
    }

    @Override
    public void delete(String token) {
        redisTemplate.delete(token);
    }

    public <T> T get(String token, Class<T> clazz) {
        return clazz.cast(redisTemplate.opsForValue().get(token));
    }

    public Long getTimeToExpire(String token, TimeUnit timeUnit) {
        redisTemplate.getExpire(token, TimeUnit.SECONDS);
        return null;
    }
}

