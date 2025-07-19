package com.quma.quma_shopify_backend.Repositories;

import com.quma.quma_shopify_backend.Models.ProductModel;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HomeRepository extends MongoRepository<ProductModel, String> {

}
