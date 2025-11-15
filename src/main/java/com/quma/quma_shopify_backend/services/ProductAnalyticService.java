package com.quma.quma_shopify_backend.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.quma.quma_shopify_backend.models.dtos.HomeAnalyticsResponseDTO;
import com.quma.quma_shopify_backend.models.dtos.ProductAnalyticRequest;
import com.quma.quma_shopify_backend.models.mongo.ProductAnalytic;
import com.quma.quma_shopify_backend.services.implementations.RedisStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductAnalyticService {

    private final RedisStore redisStore;
    private final MongoTemplate mongoTemplate;
    ExecutorService analyticsExecutor = Executors.newFixedThreadPool(6);

    @Async("analyticsExecutor")
    public void recordEvents(List<ProductAnalyticRequest> requests) {
        if (requests == null || requests.isEmpty())
            return;

        // Truncate to start of day for day-wise counts
        Instant today = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.DAYS);

        // 1️⃣ Aggregate requests in-memory by user+product+identifier
        Map<String, ProductAnalytic> aggregatedMap = new HashMap<>();
        for (ProductAnalyticRequest req : requests) {
            String key = req.getUsername() + "_" + req.getProductId() + "_" + req.getIdentifier();

            ProductAnalytic analytic = aggregatedMap.computeIfAbsent(key, k -> ProductAnalytic.builder()
                    .username(req.getUsername())
                    .productId(req.getProductId())
                    .identifier(req.getIdentifier())
                    .imageUrl(req.getImageUrl())
                    .views(0).favorites(0).addedToCart(0).orders(0).searches(0)
                    .averageRating(0).ratingCount(0)
                    .categories(req.getCategories() != null ? req.getCategories() : Collections.emptyList())
                    .date(today)
                    .lastUpdated(Instant.now())
                    .build());

            // Set count = 1 for the event type
            switch (req.getEventType()) {
                case VIEW -> analytic.setViews(1);
                case FAVORITE -> analytic.setFavorites(1);
                case ADDED_TO_CART -> analytic.setAddedToCart(1);
                case ORDERED -> analytic.setOrders(1);
                case SEARCHED -> analytic.setSearches(1);
                case RATED -> {
                    analytic.setRatingCount(1);
                    analytic.setAverageRating(req.getRatingValue() != null ? req.getRatingValue() : 0);
                }
            }

            analytic.setLastUpdated(Instant.now());
            analytic.setDate(today);
        }

        // 2️⃣ Preload existing analytics for today
        List<Criteria> criteriaList = aggregatedMap.values().stream()
                .map(a -> Criteria.where("username").is(a.getUsername())
                        .and("productId").is(a.getProductId())
                        .and("identifier").is(a.getIdentifier())
                        .and("date").is(today))
                .toList();

        List<ProductAnalytic> existingDocs = Collections.emptyList();
        if (!criteriaList.isEmpty()) {
            Criteria orCriteria = new Criteria().orOperator(criteriaList.toArray(new Criteria[0]));
            existingDocs = mongoTemplate.find(new Query(orCriteria), ProductAnalytic.class);
        }

        Map<String, ProductAnalytic> existingMap = new HashMap<>();
        for (ProductAnalytic doc : existingDocs) {
            String key = doc.getUsername() + "_" + doc.getProductId() + "_" + doc.getIdentifier();
            existingMap.put(key, doc);
        }

        // 3️⃣ Build bulk upserts with merge-safe logic
        var bulkOps = mongoTemplate.bulkOps(
                org.springframework.data.mongodb.core.BulkOperations.BulkMode.UNORDERED,
                ProductAnalytic.class);

        for (ProductAnalytic analytic : aggregatedMap.values()) {
            String key = analytic.getUsername() + "_" + analytic.getProductId() + "_" + analytic.getIdentifier();
            ProductAnalytic existing = existingMap.get(key);

            long views = (existing != null && existing.getViews() > 0) ? existing.getViews() : analytic.getViews();
            long favorites = (existing != null && existing.getFavorites() > 0) ? existing.getFavorites()
                    : analytic.getFavorites();
            long addedToCart = (existing != null && existing.getAddedToCart() > 0) ? existing.getAddedToCart()
                    : analytic.getAddedToCart();
            long orders = (existing != null && existing.getOrders() > 0) ? existing.getOrders() : analytic.getOrders();
            long searches = (existing != null && existing.getSearches() > 0) ? existing.getSearches()
                    : analytic.getSearches();

            double averageRating = analytic.getAverageRating();
            long ratingCount = analytic.getRatingCount();

            if (existing != null && analytic.getRatingCount() > 0) {
                // Merge rating for the day
                averageRating = (existing.getAverageRating() * existing.getRatingCount() + analytic.getAverageRating())
                        /
                        (existing.getRatingCount() + analytic.getRatingCount());
                ratingCount = existing.getRatingCount() + analytic.getRatingCount();
            } else if (existing != null) {
                averageRating = existing.getAverageRating();
                ratingCount = existing.getRatingCount();
            }

            Query query = new Query(Criteria.where("username").is(analytic.getUsername())
                    .and("productId").is(analytic.getProductId())
                    .and("identifier").is(analytic.getIdentifier())
                    .and("date").is(today));

            Update update = new Update()
                    .set("views", views)
                    .set("favorites", favorites)
                    .set("addedToCart", addedToCart)
                    .set("orders", orders)
                    .set("searches", searches)
                    .set("averageRating", averageRating)
                    .set("ratingCount", ratingCount)
                    .set("imageUrl", analytic.getImageUrl())
                    .set("categories", analytic.getCategories())
                    .set("lastUpdated", Instant.now())
                    .set("date", today);

            bulkOps.upsert(query, update);
        }

        // 4️⃣ Execute all updates
        bulkOps.execute();
    }

    public HomeAnalyticsResponseDTO getHomeAnalytics() {
        Map<String, String> redisKeys = Map.of(
                "mostViewed", "analytics:top:views",
                "mostFavorited", "analytics:top:favorites",
                "mostAddedToCart", "analytics:top:addedToCart",
                "mostOrdered", "analytics:top:orders",
                "mostSearched", "analytics:top:searches",
                "topRated", "analytics:top:averageRating");

        // 1️⃣ Section futures
        Map<String, CompletableFuture<List<Map<String, Object>>>> sectionFutures = new LinkedHashMap<>();
        for (var entry : redisKeys.entrySet()) {
            String section = entry.getKey();
            String redisKey = entry.getValue();

            CompletableFuture<List<Map<String, Object>>> future = CompletableFuture.supplyAsync(() -> {
                try {
                    List<Map<String, Object>> redisData = redisStore.get(redisKey, new TypeReference<>() {
                    });
                    if (redisData == null)
                        redisData = Collections.emptyList();

                    // Only keep required fields
                    return redisData.stream()
                            .map(item -> Map.of(
                                    "productId", item.get("productId"),
                                    "identifier", item.get("identifier"),
                                    "imageUrl", item.get("imageUrl")))
                            .toList();
                } catch (Exception e) {
                    log.error("❌ Error fetching section {} from Redis", section, e);
                    return Collections.emptyList();
                }
            });

            sectionFutures.put(section, future);
        }

        // 2️⃣ Top categories future
        CompletableFuture<List<Map<String, Object>>> topCategoriesFuture = CompletableFuture.supplyAsync(() -> {
            try {
                List<Map<String, Object>> topCategoriesRaw = redisStore.get("analytics:top:categories",
                        new TypeReference<>() {
                        });
                if (topCategoriesRaw == null)
                    return Collections.emptyList();

                return topCategoriesRaw.stream()
                        .map(cat -> Map.of(
                                "category", cat.get("_id"),
                                "imageUrl", cat.get("imageUrl")))
                        .toList();
            } catch (Exception e) {
                log.error("❌ Error fetching top categories from Redis", e);
                return Collections.emptyList();
            }
        });

        // 3️⃣ Combine all futures and wait
        List<CompletableFuture<?>> allFutures = new ArrayList<>(sectionFutures.values());
        allFutures.add(topCategoriesFuture);
        CompletableFuture.allOf(allFutures.toArray(new CompletableFuture[0])).join();

        // 4️⃣ Build final response
        Map<String, List<Map<String, Object>>> productSections = new LinkedHashMap<>();
        sectionFutures.forEach((key, future) -> productSections.put(key, future.join()));

        List<Map<String, Object>> topCategories = topCategoriesFuture.join();

        HomeAnalyticsResponseDTO response = new HomeAnalyticsResponseDTO();
        response.setProductSections(productSections);
        response.setTopCategories(topCategories);

        return response;
    }

}
