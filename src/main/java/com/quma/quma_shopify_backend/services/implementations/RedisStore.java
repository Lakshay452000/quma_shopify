package com.quma.quma_shopify_backend.services.implementations;

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

    @Override
    public <T> void save(String key, T value, Duration duration) {
        redisTemplate.opsForValue().set(key, value, duration);
    }

    @Override
    public void delete(String key) {
        redisTemplate.delete(key);
    }

    public <T> T get(String key, Class<T> clazz) {
        return clazz.cast(redisTemplate.opsForValue().get(key));
    }

    public Long getTimeToExpire(String key, TimeUnit timeUnit) {
        redisTemplate.getExpire(key, timeUnit);
        return null;
    }
}
