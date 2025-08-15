package com.quma.quma_shopify_backend.repositories.mongo;

import com.quma.quma_shopify_backend.models.mongo.Product;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ProductRepository extends MongoRepository<Product, String> {
    List<Product> findAllByIdIn(List<String> ids);
}

