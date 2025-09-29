package com.quma.quma_shopify_backend.repositories.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.quma.quma_shopify_backend.models.mongo.ProductInventoryDTO;

@Repository
public interface InventoryRepository extends MongoRepository<ProductInventoryDTO, String> {

}
