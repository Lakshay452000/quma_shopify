package com.quma.quma_shopify_backend.repositories.mongo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.quma.quma_shopify_backend.models.mongo.ProductInventory;

@Repository
public interface InventoryRepository extends MongoRepository<ProductInventory, String> {

    List<ProductInventory> findAllByProductIdIn(List<String> productIds);

    Optional<ProductInventory> findByProductId(String productId);

}
