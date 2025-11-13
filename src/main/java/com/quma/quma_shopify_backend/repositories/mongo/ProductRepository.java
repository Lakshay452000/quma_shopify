package com.quma.quma_shopify_backend.repositories.mongo;

import com.quma.quma_shopify_backend.models.mongo.Product;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends MongoRepository<Product, ObjectId>, ProductRepositoryCustom {
    List<Product> findAllByProductIdIn(List<String> productIds);

    Optional<Product> findByProductId(String productId);
}
