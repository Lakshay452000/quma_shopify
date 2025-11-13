package com.quma.quma_shopify_backend.repositories.mongo;

import com.quma.quma_shopify_backend.models.mongo.ProductAnalytic;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Repository
public interface ProductAnalyticRepository extends MongoRepository<ProductAnalytic, String> {

        List<ProductAnalytic> findAllByUsernameInAndProductIdInAndIdentifierIn(
                        List<String> usernames,
                        List<String> productIds,
                        List<String> identifiers);

        // 🔹 Top 10 categories for data updated after cutoff
        @Aggregation(pipeline = {
                        "{ $match: { lastUpdated: { $gte: ?0 } } }",
                        "{ $unwind: '$categories' }",
                        "{ $group: { _id: '$categories', totalOrders: { $sum: '$orders' }, totalFavorites: { $sum: '$favorites' }, totalAddedToCart: { $sum: '$addedToCart' }, totalViews: { $sum: '$views' } } }",
                        "{ $addFields: { categoryScore: { $add: [ { $multiply: [ '$totalOrders', 0.5 ] }, { $multiply: [ '$totalAddedToCart', 0.2 ] }, { $multiply: [ '$totalFavorites', 0.2 ] }, { $multiply: [ '$totalViews', 0.1 ] } ] } } }",
                        "{ $match: { categoryScore: { $gt: 0 } } }",
                        "{ $sort: { categoryScore: -1 } }",
                        "{ $limit: 10 }"
        })
        List<Map<String, Object>> findTopCategoriesAfterCutoff(Instant cutoff);

        // 🔹 Fetch a random image for a given category
        @Aggregation(pipeline = {
                        "{ $match: { 'categories': ?0, 'imageUrl': { $exists: true, $ne: null } } }",
                        "{ $sample: { size: 1 } }",
                        "{ $project: { imageUrl: 1, _id: 0 } }"
        })
        List<Map<String, Object>> findRandomImageForCategory(String category);
}
