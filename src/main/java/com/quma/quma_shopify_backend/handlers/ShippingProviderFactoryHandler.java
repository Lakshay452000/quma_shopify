package com.quma.quma_shopify_backend.handlers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import com.quma.quma_shopify_backend.interfaces.IShippingProvider;

import java.util.Map;

@Component
public class ShippingProviderFactoryHandler {

    @Autowired
    private ApplicationContext context;

    /**
     * Returns an IShippingProvider by name at runtime.
     */
    public IShippingProvider getProvider(String name) {
        Map<String, IShippingProvider> beans = context.getBeansOfType(IShippingProvider.class);
        return beans.values().stream()
                .filter(p -> p.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Shipping provider not found: " + name));
    }
}
