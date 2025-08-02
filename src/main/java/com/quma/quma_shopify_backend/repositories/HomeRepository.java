package com.quma.quma_shopify_backend.repositories;

import com.quma.quma_shopify_backend.models.mongo.ProductModel;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HomeRepository extends MongoRepository<ProductModel, String> {

}
