package com.quma.quma_shopify_backend.repositories.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.quma.quma_shopify_backend.models.mongo.CartItemMongoDTO;

@Repository
public interface CartRepository extends MongoRepository<CartItemMongoDTO, String> {
    CartItemMongoDTO findByUsername(String username);
}
