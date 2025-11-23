package com.quma.quma_shopify_backend.services;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.mongodb.core.FindAndModifyOptions;

import com.quma.quma_shopify_backend.exceptions.ApiException;
import com.quma.quma_shopify_backend.models.mongo.Product;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductStockService {

    private final MongoTemplate mongoTemplate;

    /**
     * Atomically reduce stock
     */
    public void reduceStock(String productId, String identifier, int quantity) {

        Query query = new Query()
                .addCriteria(Criteria.where("productId").is(productId))
                .addCriteria(Criteria.where("variants.identifier").is(identifier))
                .addCriteria(Criteria.where("variants.stock").gte(quantity));

        Update update = new Update()
                .inc("variants.$.stock", -quantity)
                .currentTimestamp("updatedAt");

        Product updated = mongoTemplate.findAndModify(
                query,
                update,
                FindAndModifyOptions.options().returnNew(true),
                Product.class);

        if (updated == null) {
            throw new ApiException(
                    "Insufficient stock for product " + productId +
                            " (variant: " + identifier + ")",
                    400);
        }
    }

    /**
     * Atomically add stock back (for expired/cancelled orders)
     */
    public void addStock(String productId, String identifier, int quantity) {

        Query query = new Query()
                .addCriteria(Criteria.where("productId").is(productId))
                .addCriteria(Criteria.where("variants.identifier").is(identifier));

        Update update = new Update()
                .inc("variants.$.stock", quantity)
                .currentTimestamp("updatedAt");

        mongoTemplate.findAndModify(
                query,
                update,
                FindAndModifyOptions.options().returnNew(true),
                Product.class);
    }
}
