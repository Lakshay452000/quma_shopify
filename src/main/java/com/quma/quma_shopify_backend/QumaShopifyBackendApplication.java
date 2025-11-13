package com.quma.quma_shopify_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableMongoRepositories(basePackages = "com.quma.quma_shopify_backend.repositories.mongo")
public class QumaShopifyBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(QumaShopifyBackendApplication.class, args);
    }

}
