package com.quma.quma_shopify_backend.services.implementations;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quma.quma_shopify_backend.interfaces.IStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Service
public class RedisStore implements IStore {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public <T> void save(String key, T value, Duration duration) {
        redisTemplate.opsForValue().set(key, value, duration);
    }

    @Override
    public void delete(String key) {
        redisTemplate.delete(key);
    }

    public <T> T get(String key, Class<T> clazz) {
        Object value = redisTemplate.opsForValue().get(key);
        return value == null ? null : clazz.cast(value);
    }

    public <T> T get(String key, TypeReference<T> typeReference) {
        Object value = redisTemplate.opsForValue().get(key);
        if (value == null)
            return null;
        return objectMapper.convertValue(value, typeReference);
    }

    public Long getTimeToExpire(String key, TimeUnit timeUnit) {
        return redisTemplate.getExpire(key, timeUnit);
    }
}
