package com.quma.quma_shopify_backend.interfaces;

import java.time.Duration;

public interface IStore {

    <T> void save(String key, T value, Duration duration);

    void delete(String key);

    <T> T get(String key, Class<T> clazz);
}
