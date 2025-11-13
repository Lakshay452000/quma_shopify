package com.quma.quma_shopify_backend.repositories.mongo;

import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import com.quma.quma_shopify_backend.models.mongo.Order;

import java.time.Instant;

@RequiredArgsConstructor
public class OrderRepositoryCustomImpl implements OrderRepositoryCustom {

        private final MongoTemplate mongoTemplate;

        @Override
        public long expireOrders() {
                Query query = new Query(Criteria.where("orderStatus").is("ACTIVE")
                                .and("orderPaymentStatus").is("PENDING")
                                .and("expiresAt").lt(Instant.now()));

                // Update orderStatus to EXPIRED
                Update update = new Update().set("orderStatus", "EXPIRED");

                // Execute update
                var result = mongoTemplate.updateMulti(query, update, Order.class);
                return result.getModifiedCount();
        }

        @Override
        public long resetProcessingOrders() {
                Instant now = Instant.now();
                Instant sevenMinutesAgo = now.minusSeconds(7 * 60);

                // 1️⃣ If expired: mark as PENDING & EXPIRED
                Query expiredQuery = new Query(Criteria.where("orderStatus").is("ACTIVE")
                                .and("orderPaymentStatus").is("PROCESSING")
                                .and("expiresAt").lte(now));
                Update expiredUpdate = new Update()
                                .set("orderPaymentStatus", "PENDING")
                                .set("orderStatus", "EXPIRED");
                long expiredCount = mongoTemplate.updateMulti(expiredQuery, expiredUpdate, Order.class)
                                .getModifiedCount();

                // 2️⃣ If not expired and older than 7 min: mark PROCESSING → PENDING only
                Query resetQuery = new Query(Criteria.where("orderStatus").is("ACTIVE")
                                .and("orderPaymentStatus").is("PROCESSING")
                                .and("expiresAt").gt(now)
                                .and("updatedAt").lt(sevenMinutesAgo));
                Update resetUpdate = new Update().set("orderPaymentStatus", "PENDING");
                long resetCount = mongoTemplate.updateMulti(resetQuery, resetUpdate, Order.class).getModifiedCount();

                return expiredCount + resetCount;
        }

}
