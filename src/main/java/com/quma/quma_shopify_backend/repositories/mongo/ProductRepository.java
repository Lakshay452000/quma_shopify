package com.quma.quma_shopify_backend.repositories.mongo;

import com.quma.quma_shopify_backend.models.mongo.Product;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ProductRepository extends MongoRepository<Product, ObjectId> {
    List<Product> findAllByIdIn(List<ObjectId> ids);
}

