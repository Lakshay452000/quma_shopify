package com.quma.quma_shopify_backend.cron;

import com.quma.quma_shopify_backend.repositories.mongo.ProductAnalyticRepository;
import com.quma.quma_shopify_backend.services.implementations.RedisStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductAnalyticCron {

        private final MongoTemplate mongoTemplate;
        private final RedisStore redisStore;
        private final ProductAnalyticRepository productAnalyticRepository;

        // ✅ Cron schedule - runs every midnight
        @Scheduled(cron = "0 0 0 * * *")
        public void scheduledUpdate() {
                runAnalyticsAggregation();
        }

        // ✅ Public method to call manually (from controller or admin panel)
        public void runAnalyticsAggregation() {
                log.info("🚀 Starting product analytics aggregation manually or via cron...");

                Instant cutoff = Instant.now().minus(7, ChronoUnit.DAYS);

                calculateTop10("views", cutoff);
                calculateTop10("favorites", cutoff);
                calculateTop10("addedToCart", cutoff);
                calculateTop10("orders", cutoff);
                calculateTop10("searches", cutoff);
                calculateTop10("averageRating", cutoff);
                calculateTopCategories(cutoff);

                log.info("✅ All analytics rankings updated successfully in Redis");
        }

        private void calculateTop10(String metric, Instant cutoff) {
                try {
                        Aggregation aggregation = Aggregation.newAggregation(
                                        Aggregation.match(Criteria.where("lastUpdated").gte(cutoff)),
                                        Aggregation.group("productId")
                                                        .first("identifier").as("identifier")
                                                        .first("imageUrl").as("imageUrl")
                                                        .sum(metric).as("total"),
                                        Aggregation.match(Criteria.where("total").gt(0)), // ✅ only >0 counts
                                        Aggregation.sort(Sort.by(Sort.Direction.DESC, "total")),
                                        Aggregation.limit(50));

                        List<Document> results = mongoTemplate
                                        .aggregate(aggregation, "product_analytics", Document.class)
                                        .getMappedResults();

                        if (results.isEmpty()) {
                                log.warn("⚠️ No data found for metric {}", metric);
                                return;
                        }

                        double lambda = 0.1;
                        List<Map<String, Object>> top10 = results.stream()
                                        .map(doc -> {
                                                long total = doc.getLong("total");
                                                double decayedScore = total * Math.exp(-lambda *
                                                                Duration.between(cutoff, Instant.now()).toDays());

                                                Map<String, Object> map = new java.util.HashMap<>();
                                                map.put("productId", doc.getString("_id"));
                                                map.put("identifier", doc.getString("identifier"));
                                                map.put("imageUrl", doc.getString("imageUrl"));
                                                map.put("score", decayedScore);
                                                return map;
                                        })
                                        .filter(item -> (double) item.get("score") > 0) // ✅ ensure score > 0
                                        .sorted((a, b) -> Double.compare((double) b.get("score"),
                                                        (double) a.get("score")))
                                        .limit(10)
                                        .collect(Collectors.toList());

                        if (!top10.isEmpty()) {
                                redisStore.save("analytics:top:" + metric, top10, Duration.ofDays(7));
                                log.info("✅ Saved top 10 {} to Redis ({} items)", metric, top10.size());
                        } else {
                                log.warn("⚠️ No non-zero entries found for metric {}", metric);
                        }
                } catch (Exception e) {
                        log.error("❌ Failed to compute top 10 for {}", metric, e);
                }
        }

        private void calculateTopCategories(Instant cutoff) {
                try {
                        List<Map<String, Object>> topCategories = productAnalyticRepository
                                        .findTopCategoriesAfterCutoff(cutoff);

                        // 🔹 Attach one random image to each category
                        List<Map<String, Object>> enriched = topCategories.stream().map(cat -> {
                                String category = (String) cat.get("_id");
                                List<Map<String, Object>> imageResult = productAnalyticRepository
                                                .findRandomImageForCategory(category);

                                cat.put("imageUrl", imageResult.isEmpty() ? null : imageResult.get(0).get("imageUrl"));
                                return cat;
                        }).toList();

                        redisStore.save("analytics:top:categories", enriched, Duration.ofDays(7));
                        log.info("✅ Aggregated top 10 categories after cutoff {} and saved to Redis", cutoff);
                } catch (Exception e) {
                        log.error("❌ Failed to compute top categories", e);
                }
        }

}
