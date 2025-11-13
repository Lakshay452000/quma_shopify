package com.quma.quma_shopify_backend.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quma.quma_shopify_backend.enums.AnalyticsEventType;
import com.quma.quma_shopify_backend.enums.SortType;
import com.quma.quma_shopify_backend.exceptions.ApiException;
import com.quma.quma_shopify_backend.models.dtos.ElasticVariantsResponseDTO;
import com.quma.quma_shopify_backend.models.dtos.ProductAnalyticRequest;
import com.quma.quma_shopify_backend.models.dtos.ProductSearchRequestDTO;
import com.quma.quma_shopify_backend.models.elastic.ProductElasticDocument;
import com.quma.quma_shopify_backend.models.elastic.ProductElasticDocumentDTO;
import com.quma.quma_shopify_backend.models.elastic.ProductElasticResponseDocument;
import com.quma.quma_shopify_backend.models.mongo.Product;
import com.quma.quma_shopify_backend.models.mongo.Variant;
import com.quma.quma_shopify_backend.models.mongo.Wishlist;
import com.quma.quma_shopify_backend.repositories.mongo.ProductRepository;
import com.quma.quma_shopify_backend.repositories.mongo.WishlistRepository;
import com.quma.quma_shopify_backend.utilities.Constants;
import com.quma.quma_shopify_backend.utilities.ElasticProductUtils;
import com.quma.quma_shopify_backend.utilities.UserContext;

import org.elasticsearch.action.update.UpdateRequest;

import org.elasticsearch.search.aggregations.AggregationBuilders;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.get.MultiGetItemResponse;
import org.elasticsearch.action.get.MultiGetRequest;
import org.elasticsearch.action.get.MultiGetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.Fuzziness;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.collapse.CollapseBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.elasticsearch.index.query.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Slf4j
@SuppressWarnings("deprecation")
public class ElasticSearchService {

    @Autowired
    private RestHighLevelClient restHighLevelClient;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private WishlistRepository wishlistRepository;
    @Autowired
    private ProductAnalyticService analyticService;

    public ProductElasticDocument getProductElasticDocument(Product product) {
        ProductElasticDocument doc = objectMapper.convertValue(product, ProductElasticDocument.class);
        return doc;
    }

    public void indexProducts(List<String> productIds) {
        try {
            // 1️⃣ Fetch products from Mongo
            List<Product> products = productRepository.findAllByProductIdIn(productIds);

            if (products.isEmpty()) {
                log.warn("No products found for IDs: {}", productIds);
                return;
            }

            // 2️⃣ Prepare bulk request
            BulkRequest bulkRequest = new BulkRequest();

            for (Product product : products) {
                List<Variant> variants = product.getVariants();
                if (variants == null || variants.isEmpty()) {
                    log.warn("Product {} has no variants, skipping.", product.getProductId());
                    continue;
                }

                // 3️⃣ Create ES doc for each variant
                for (int i = 0; i < variants.size(); i++) {
                    Variant variant = variants.get(i);
                    ProductElasticDocument doc = new ProductElasticDocument();
                    doc.setProductId(product.getProductId());
                    doc.setIdentifier(variant.getIdentifier());
                    doc.setTitle(product.getTitle());
                    doc.setBrand(product.getBrand());
                    doc.setCategories(product.getCategories());
                    doc.setTypes(product.getTypes());
                    doc.setTags(product.getTags());
                    doc.setColor(variant.getColor());
                    doc.setSize(variant.getSize());
                    doc.setWeight(variant.getWeight());
                    doc.setMaterials(variant.getMaterials());
                    doc.setImages(variant.getImages());
                    doc.setPrice(variant.getPrice());
                    doc.setDiscountedPrice(variant.getDiscountedPrice());
                    doc.setAverageRating(product.getAverageRating());
                    doc.setTotalBuyers(product.getTotalBuyers());
                    doc.setIsActive(product.getIsActive());
                    doc.setCreatedAt(product.getCreatedAt());
                    doc.setUpdatedAt(product.getUpdatedAt());

                    String json = objectMapper.writeValueAsString(doc);

                    IndexRequest indexRequest = new IndexRequest(Constants.ELASTIC_PRODUCT_INDEX_NAME)
                            .id(product.getProductId() + "-" + variant.getIdentifier()) // ✅ unique per variant
                            .source(json, XContentType.JSON);

                    bulkRequest.add(indexRequest);
                }
            }

            // 4️⃣ Bulk index to Elasticsearch
            if (bulkRequest.numberOfActions() > 0) {
                BulkResponse response = restHighLevelClient.bulk(bulkRequest, RequestOptions.DEFAULT);
                if (response.hasFailures()) {
                    log.error("Bulk indexing completed with failures: {}", response.buildFailureMessage());
                } else {
                    log.info("Successfully indexed {} variant documents to Elasticsearch.",
                            bulkRequest.numberOfActions());
                }
            }

        } catch (Exception e) {
            log.error("Error during bulk indexing: {}", e.getMessage(), e);
            throw new ApiException("Failed to index product variants in Elasticsearch", 500);
        }
    }

    public List<ProductElasticDocument> getProductsByIds(List<String> ids) throws IOException {
        if (CollectionUtils.isEmpty(ids)) {
            return Collections.emptyList();
        }

        MultiGetRequest multiGetRequest = new MultiGetRequest();
        for (String id : ids) {
            multiGetRequest.add(new MultiGetRequest.Item(Constants.ELASTIC_PRODUCT_INDEX_NAME, id));
        }

        MultiGetResponse multiGetResponse = restHighLevelClient.mget(multiGetRequest, RequestOptions.DEFAULT);

        List<ProductElasticDocument> products = new ArrayList<>();
        for (MultiGetItemResponse itemResponse : multiGetResponse.getResponses()) {
            if (itemResponse.getResponse() != null && itemResponse.getResponse().isExists()) {
                Map<String, Object> sourceMap = itemResponse.getResponse().getSourceAsMap();
                ProductElasticDocument product = objectMapper.convertValue(sourceMap, ProductElasticDocument.class);
                product.setId(itemResponse.getId());
                products.add(product);
            }
        }
        return products;
    }

    public ProductElasticResponseDocument searchProducts(ProductSearchRequestDTO request)
            throws ApiException, IOException {
        try {
            String normalized = normalizeQuery(request.getSearchTerm());

            // ---------- Base query for hits ----------
            BoolQueryBuilder bool = QueryBuilders.boolQuery()
                    .filter(QueryBuilders.termQuery("isActive", true));

            if (StringUtils.isNotBlank(normalized)) {
                bool.must(QueryBuilders.multiMatchQuery(normalized,
                        "title", "categories", "types", "brand", "tags",
                        "title.ngram", "categories.ngram", "types.ngram", "brand.ngram", "tags.ngram")
                        .type(MultiMatchQueryBuilder.Type.BEST_FIELDS)
                        .fuzziness(Fuzziness.AUTO));
            }

            // ---------- Apply user-selected filters ----------
            if (request.getFilters() != null && !request.getFilters().isEmpty()) {
                BoolQueryBuilder filterBool = QueryBuilders.boolQuery();
                request.getFilters().forEach((k, v) -> {
                    if (v != null && !v.isEmpty()) {
                        String normalizedKey = Character.toLowerCase(k.charAt(0)) + k.substring(1);
                        BoolQueryBuilder perFieldBool = QueryBuilders.boolQuery();
                        v.forEach(val -> perFieldBool.should(QueryBuilders.termQuery(normalizedKey, val)));
                        filterBool.must(perFieldBool);
                    }
                });
                bool.filter(filterBool);
            }

            boolean isFirstPage = request.isInitialLoad();

            // ---------- Paginated search query with collapse ----------
            SearchSourceBuilder ssb = new SearchSourceBuilder()
                    .query(bool)
                    .size(request.getPageSize())
                    .sort("_score", SortOrder.DESC)
                    .sort(StringUtils.defaultIfBlank(request.getSortBy(), "updatedAt"),
                            request.getSortType() == SortType.ASC ? SortOrder.ASC : SortOrder.DESC)
                    .sort("_id", SortOrder.ASC)
                    .collapse(new CollapseBuilder("productId"));

            if (request.getSortValues() != null && request.getSortValues().length > 0) {
                ssb.searchAfter(request.getSortValues());
            }

            SearchRequest searchRequest = new SearchRequest(Constants.ELASTIC_PRODUCT_INDEX_NAME).source(ssb);
            SearchResponse resp = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);

            // ---------- Parse hits ----------
            List<ProductElasticDocumentDTO> products = new ArrayList<>();
            List<String> productIds = new ArrayList<>();
            Object[] lastSortValues = null;
            SearchHit[] hits = resp.getHits().getHits();
            if (hits != null && hits.length > 0) {
                for (SearchHit hit : hits) {
                    ProductElasticDocumentDTO p = objectMapper.convertValue(hit.getSourceAsMap(),
                            ProductElasticDocumentDTO.class);
                    lastSortValues = hit.getSortValues();
                    products.add(p);
                    productIds.add(p.getProductId() + "$" + p.getIdentifier());
                }
            }

            if (StringUtils.isNotBlank(normalized)) {
                List<ProductAnalyticRequest> analyticsToRecord = products.stream()
                        .map(p -> ProductAnalyticRequest.builder()
                                .username(UserContext.get().getUsername())
                                .productId(p.getProductId())
                                .identifier(p.getIdentifier())
                                .imageUrl(p.getImages().get(0))
                                .categories(p.getCategories())
                                .eventType(AnalyticsEventType.SEARCHED)
                                .build())
                        .toList();

                analyticService.recordEvents(analyticsToRecord);
            }
            String username = UserContext.get().getUsername();
            List<Wishlist> wishlists = wishlistRepository.findAllByUsernameAndProductIdIn(username, productIds);

            Set<String> wishlistedIds = wishlists.stream()
                    .map(Wishlist::getProductId)
                    .collect(Collectors.toSet());

            products.forEach(p -> p.setWishlisted(wishlistedIds.contains(p.getProductId() + "$" + p.getIdentifier())));

            ProductElasticResponseDocument out = new ProductElasticResponseDocument();
            out.setProducts(products);
            out.setSortValues(lastSortValues);

            // ---------- Aggregations (filters + variant info) ----------
            if (isFirstPage) {
                BoolQueryBuilder aggBool = QueryBuilders.boolQuery();
                if (StringUtils.isNotBlank(normalized)) {
                    aggBool.must(QueryBuilders.multiMatchQuery(normalized,
                            "title", "categories", "types", "brand", "tags",
                            "title.ngram", "categories.ngram", "types.ngram", "brand.ngram", "tags.ngram")
                            .type(MultiMatchQueryBuilder.Type.BEST_FIELDS)
                            .fuzziness(Fuzziness.AUTO));
                }

                SearchSourceBuilder aggSSB = new SearchSourceBuilder().query(aggBool).size(0);

                // ---------- Top-level + variant filters ----------
                List<String> filterFields = new ArrayList<>(ElasticProductUtils.getTopLevelFilters());
                for (String field : filterFields) {
                    aggSSB.aggregation(AggregationBuilders.terms(field + "_agg")
                            .field(field)
                            .size(100));
                }

                SearchRequest aggRequest = new SearchRequest(Constants.ELASTIC_PRODUCT_INDEX_NAME).source(aggSSB);
                SearchResponse aggResp = restHighLevelClient.search(aggRequest, RequestOptions.DEFAULT);

                // Parse aggregations
                LinkedHashMap<String, LinkedHashSet<String>> filtersSet = new LinkedHashMap<>();
                for (String field : filterFields) {
                    if ("tags".equalsIgnoreCase(field) || "images".equalsIgnoreCase(field))
                        continue;

                    LinkedHashSet<String> values = new LinkedHashSet<>();
                    Terms agg = aggResp.getAggregations().get(field + "_agg");
                    if (agg != null) {
                        agg.getBuckets().forEach(bucket -> {
                            String key = bucket.getKeyAsString();
                            if (key != null && !key.isEmpty())
                                values.add(key);
                        });
                    }
                    filtersSet.put(field, values);
                }

                // Remove search term from filters
                if (StringUtils.isNotBlank(normalized)) {
                    String searchTermLower = normalized.toLowerCase(Locale.ROOT);
                    filtersSet.values().forEach(set -> set.removeIf(v -> v.equalsIgnoreCase(searchTermLower)));
                }

                // ---------- Final filters & quickFilters ----------
                Map<String, List<String>> filters = new LinkedHashMap<>();
                filtersSet.forEach((k, v) -> {
                    String cleanKey = k.replaceAll("\\.keyword$", ""); // remove .keyword if present
                    if (!cleanKey.isEmpty())
                        cleanKey = Character.toUpperCase(cleanKey.charAt(0)) + cleanKey.substring(1);
                    filters.put(cleanKey, new ArrayList<>(v));
                });
                out.setFilters(filters);

                Map<String, List<String>> quickFilters = new LinkedHashMap<>();
                for (String key : filtersSet.keySet()) {
                    String cleanKey = key.replaceAll("\\.keyword$", "");
                    cleanKey = Character.toUpperCase(cleanKey.charAt(0)) + cleanKey.substring(1);
                    List<String> values = new ArrayList<>(filtersSet.get(key));
                    if (!values.isEmpty())
                        quickFilters.put(cleanKey, values.stream().limit(7).toList());
                }
                out.setQuickFilters(quickFilters);
            }

            return out;

        } catch (Exception e) {
            log.error("Error occurred while processing search response", e);
            throw new ApiException("Search failed: " + e.getMessage(), 500);
        }
    }

    // ---------------- Helper: normalize query ----------------
    private static final Pattern REPEAT_RUN = Pattern.compile("(\\p{L}|\\p{N})\\1{2,}");

    private String normalizeQuery(String q) {
        if (q == null)
            return "";
        String s = q.trim().toLowerCase(Locale.ROOT);
        s = REPEAT_RUN.matcher(s).replaceAll("$1$1");
        return s.length() > 64 ? s.substring(0, 64) : s;
    }

    public List<String> getSuggestions(String indexName, String input, int size) throws IOException {
        if (input == null || input.isBlank()) {
            return Collections.emptyList();
        }

        String inputLower = input.toLowerCase(Locale.ROOT);

        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.query(
                QueryBuilders.multiMatchQuery(inputLower,
                        "title.ngram", "title.fuzzy",
                        "tags.ngram", "tags.fuzzy",
                        "brand.ngram", "brand.fuzzy",
                        "categories.ngram", "categories.fuzzy",
                        "types.ngram", "types.fuzzy")
                        .type(MultiMatchQueryBuilder.Type.BEST_FIELDS)
                        .fuzziness(Fuzziness.AUTO)
                        .prefixLength(1)
                        .operator(Operator.OR));

        sourceBuilder.size(100);
        sourceBuilder.fetchSource(new String[] { "title", "tags", "brand", "categories", "types" }, null);

        SearchRequest searchRequest = new SearchRequest(indexName);
        searchRequest.source(sourceBuilder);

        SearchResponse response = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);

        List<SuggestionItem> candidates = new ArrayList<>();
        for (SearchHit hit : response.getHits()) {
            Map<String, Object> source = hit.getSourceAsMap();

            if (source.get("title") != null)
                candidates.add(new SuggestionItem(source.get("title").toString(), "title"));

            if (source.get("tags") instanceof List<?>) {
                ((List<?>) source.get("tags")).forEach(t -> candidates.add(new SuggestionItem(t.toString(), "tags")));
            }

            if (source.get("brand") != null)
                candidates.add(new SuggestionItem(source.get("brand").toString(), "brand"));

            if (source.get("categories") instanceof List<?>) {
                ((List<?>) source.get("categories"))
                        .forEach(c -> candidates.add(new SuggestionItem(c.toString(), "categories")));
            }
            if (source.get("types") instanceof List<?>) {
                ((List<?>) source.get("types")).forEach(t -> candidates.add(new SuggestionItem(t.toString(), "types")));
            }
        }

        // Deduplicate
        Map<String, SuggestionItem> uniqueMap = new LinkedHashMap<>();
        for (SuggestionItem item : candidates) {
            uniqueMap.putIfAbsent(item.text, item);
        }

        // Rank and return top suggestions
        return uniqueMap.values().stream()
                .sorted((a, b) -> Integer.compare(
                        getMatchScore(b.text.toLowerCase(Locale.ROOT), inputLower, b.field),
                        getMatchScore(a.text.toLowerCase(Locale.ROOT), inputLower, a.field)))
                .limit(size)
                .map(item -> item.text)
                .collect(Collectors.toList());
    }

    private static class SuggestionItem {
        String text;
        String field;

        SuggestionItem(String text, String field) {
            this.text = text;
            this.field = field;
        }
    }

    // ---------------- Improved fuzzy + prefix scoring ----------------
    private int getMatchScore(String text, String input, String field) {
        int distance = levenshteinDistance(text, input);
        int baseScore;

        if (text.equals(input))
            baseScore = 100; // exact
        else if (text.startsWith(input))
            baseScore = 85; // prefix
        else if (distance <= 1)
            baseScore = 75; // close fuzzy
        else if (distance == 2)
            baseScore = 60; // looser fuzzy
        else if (text.contains(input))
            baseScore = 50; // substring
        else
            baseScore = Math.max(10, 30 - distance * 5);

        switch (field) {
            case "title":
                return baseScore + 20;
            case "categories":
                return baseScore + 15;
            case "brand":
                return baseScore + 10;
            default:
                return baseScore;
        }
    }

    // ---------------- Helper for fuzzy distance ----------------
    private int levenshteinDistance(String a, String b) {
        int[][] dp = new int[a.length() + 1][b.length() + 1];
        for (int i = 0; i <= a.length(); i++) {
            for (int j = 0; j <= b.length(); j++) {
                if (i == 0)
                    dp[i][j] = j;
                else if (j == 0)
                    dp[i][j] = i;
                else
                    dp[i][j] = Math.min(
                            Math.min(dp[i - 1][j - 1] + (a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1),
                                    dp[i - 1][j] + 1),
                            dp[i][j - 1] + 1);
            }
        }
        return dp[a.length()][b.length()];
    }

    public void updateFields(String indexName, String documentId, Map<String, Object> fields) {
        try {
            if (fields == null || fields.isEmpty())
                return;

            UpdateRequest request = new UpdateRequest(indexName, documentId)
                    .doc(fields);
            restHighLevelClient.update(request, RequestOptions.DEFAULT);
        } catch (Exception e) {
            // Log error; in real systems, you might push to a retry queue
            System.err.println("Failed to update Elastic document " + documentId + ": " + e.getMessage());
        }
    }

    public List<ElasticVariantsResponseDTO> getVariantsByBaseProductId(String baseProductId) throws IOException {
        if (StringUtils.isBlank(baseProductId)) {
            return Collections.emptyList();
        }

        // ✅ Define which fields to fetch from _source
        String[] includedFields = {
                "productId",
                "baseProductId",
                "price",
                "discountedPrice",
                "images",
                "color"
        };

        // ✅ Build query
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder()
                .query(QueryBuilders.boolQuery()
                        .filter(QueryBuilders.termQuery("baseProductId", baseProductId))
                        .filter(QueryBuilders.termQuery("isActive", true)))
                .size(50)
                .fetchSource(includedFields, new String[] {});

        SearchRequest searchRequest = new SearchRequest(Constants.ELASTIC_PRODUCT_INDEX_NAME)
                .source(sourceBuilder);

        SearchResponse response = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);

        List<ElasticVariantsResponseDTO> variants = new ArrayList<>();

        for (SearchHit hit : response.getHits().getHits()) {
            Map<String, Object> src = hit.getSourceAsMap();

            ElasticVariantsResponseDTO dto = new ElasticVariantsResponseDTO();
            dto.setProductId((String) src.get("productId"));
            dto.setBaseProductId((String) src.get("baseProductId"));
            dto.setColor((String) src.get("color"));

            // ✅ Convert numeric fields safely
            Object price = src.get("price");
            if (price != null) {
                dto.setPrice(new BigDecimal(price.toString()));
            }

            Object discountedPrice = src.get("discountedPrice");
            if (discountedPrice != null) {
                dto.setDiscountedPrice(new BigDecimal(discountedPrice.toString()));
            }

            // ✅ Extract first image only
            Object images = src.get("images");
            if (images instanceof List<?> list && !list.isEmpty()) {
                dto.setImage(list.get(0).toString());
            } else if (images instanceof String) {
                dto.setImage(images.toString());
            }

            variants.add(dto);
        }

        return variants;
    }

}