package com.quma.quma_shopify_backend.repositories.mongo;

import com.quma.quma_shopify_backend.models.mongo.Product;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

@Repository
public class ProductRepositoryCustomImpl implements ProductRepositoryCustom {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public void updateVariantWishlisted(String productId, String variantIdentifier, boolean isWishlisted) {
        Query query = new Query();
        query.addCriteria(Criteria.where("productId").is(productId)
                .and("variants.identifier").is(variantIdentifier));

        Update update = new Update().set("variants.$.isWishlisted", isWishlisted);

        mongoTemplate.updateFirst(query, update, Product.class);
    }
}
