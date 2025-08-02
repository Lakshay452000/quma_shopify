package com.quma.quma_shopify_backend.interfaces;

import java.time.Duration;


public interface ITokenStore {

    <T> void save(String token, T value, Duration duration);

    void delete(String token);

    <T> T get(String token, Class<T> clazz);
}
