package com.quma.quma_shopify_backend.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quma.quma_shopify_backend.enums.SortType;
import com.quma.quma_shopify_backend.exceptions.ApiException;
import com.quma.quma_shopify_backend.models.dtos.ProductsRequestDTO;
import com.quma.quma_shopify_backend.models.elastic.ProductElasticDocument;
import com.quma.quma_shopify_backend.models.elastic.ProductElasticResponseDocument;
import com.quma.quma_shopify_backend.models.mongo.Product;
import com.quma.quma_shopify_backend.utilities.Constants;
import com.quma.quma_shopify_backend.utilities.ElasticProductUtils;
import org.elasticsearch.action.update.UpdateRequest;

import org.elasticsearch.search.aggregations.AggregationBuilders;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.search.join.ScoreMode;
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
import org.elasticsearch.search.aggregations.bucket.nested.Nested;
import org.elasticsearch.search.aggregations.bucket.nested.NestedAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.elasticsearch.index.query.*;

import java.io.IOException;
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

    public ProductElasticDocument getProductElasticDocuments(Product product) {
        ProductElasticDocument doc = objectMapper.convertValue(product, ProductElasticDocument.class);
        return doc;
    }

    public void indexProducts(List<ProductElasticDocument> products) throws Exception {
        try {
            BulkRequest bulkRequest = new BulkRequest();
            for (ProductElasticDocument doc : products) {
                String json = objectMapper.writeValueAsString(doc);
                IndexRequest request = new IndexRequest(Constants.ELASTIC_PRODUCT_INDEX_NAME)
                        .id(doc.getId()) // This should be productId-variantId
                        .source(json, XContentType.JSON);
                bulkRequest.add(request);
            }
            BulkResponse bulkResponse = restHighLevelClient.bulk(bulkRequest, RequestOptions.DEFAULT);
            if (bulkResponse.hasFailures()) {
                log.error("Some documents failed to index: {}", bulkResponse.buildFailureMessage());
            } else {
                log.info("Indexed {} documents to Elastic", products.size());
            }
        } catch (Exception e) {
            log.error("Error indexing products to Elastic: {}", e.getMessage());
            throw e;
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

    public ProductElasticResponseDocument searchProducts(ProductsRequestDTO request) throws ApiException, IOException {
        try {
            String normalized = normalizeQuery(request.getSearchTerm());

            // ---------- Base query for hits ----------
            BoolQueryBuilder bool = QueryBuilders.boolQuery()
                    .filter(QueryBuilders.termQuery("isActive", true));

            if (StringUtils.isNotBlank(normalized)) {
                bool.must(QueryBuilders.multiMatchQuery(normalized,
                        "title", "description", "categories", "types", "brand", "tags",
                        "title.ngram", "description.ngram", "tags.ngram")
                        .type(MultiMatchQueryBuilder.Type.BEST_FIELDS)
                        .fuzziness(Fuzziness.AUTO));
            }

            // ---------- Apply user-selected filters (AND between fields, OR within same
            // field) ----------
            if (request.getFilters() != null && !request.getFilters().isEmpty()) {
                BoolQueryBuilder filterBool = QueryBuilders.boolQuery();

                Map<String, List<String>> normalizedFilters = new HashMap<>();
                request.getFilters().forEach((k, v) -> {
                    if (v != null && !v.isEmpty()) {
                        String normalizedKey = Character.toLowerCase(k.charAt(0)) + k.substring(1);
                        normalizedFilters.put(normalizedKey, v);
                    }
                });
                for (Map.Entry<String, List<String>> entry : normalizedFilters.entrySet()) {
                    String field = entry.getKey();
                    List<String> values = entry.getValue();

                    if (values != null && !values.isEmpty()) {
                        BoolQueryBuilder perFieldBool = QueryBuilders.boolQuery();
                        values.forEach(v -> perFieldBool.should(QueryBuilders.termQuery(field, v)));

                        if (ElasticProductUtils.getNestedFilters().contains(field)) {
                            filterBool.must(QueryBuilders.nestedQuery("variants", perFieldBool, ScoreMode.None));
                        } else {
                            filterBool.must(perFieldBool);
                        }
                    }
                }

                bool.filter(filterBool);
            }

            boolean isFirstPage = request.isInitialLoad();

            // ---------- Paginated search query ----------
            SearchSourceBuilder ssb = new SearchSourceBuilder()
                    .query(bool)
                    .size(request.getPageSize())
                    .sort("_score", SortOrder.DESC)
                    .sort(StringUtils.defaultIfBlank(request.getSortBy(), "updatedAt"),
                            request.getSortType() == SortType.ASC ? SortOrder.ASC : SortOrder.DESC)
                    .sort("_id", SortOrder.ASC);

            if (request.getSortValues() != null && request.getSortValues().length > 0) {
                ssb.searchAfter(request.getSortValues());
            }

            SearchRequest searchRequest = new SearchRequest(Constants.ELASTIC_PRODUCT_INDEX_NAME).source(ssb);
            SearchResponse resp = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);

            // ---------- Parse hits ----------
            List<ProductElasticDocument> products = new ArrayList<>();
            Object[] lastSortValues = null;
            SearchHit[] hits = resp.getHits().getHits();
            if (hits == null || hits.length == 0) {
                ProductElasticResponseDocument empty = new ProductElasticResponseDocument();
                empty.setProducts(Collections.emptyList());
                empty.setSortValues(null);
                empty.setFilters(Collections.emptyMap());
                empty.setQuickFilters(Collections.emptyMap());
                return empty;
            }

            for (SearchHit hit : resp.getHits().getHits()) {
                ProductElasticDocument p = objectMapper.convertValue(hit.getSourceAsMap(),
                        ProductElasticDocument.class);
                p.setId(hit.getId());
                lastSortValues = hit.getSortValues();
                products.add(p);
            }

            ProductElasticResponseDocument out = new ProductElasticResponseDocument();
            out.setProducts(products);
            out.setSortValues(lastSortValues);

            // ---------- Aggregations (filters & quick filters) ----------
            if (isFirstPage) {
                // Aggregation query ignores selected filters, uses only search term
                BoolQueryBuilder aggBool = QueryBuilders.boolQuery();
                if (StringUtils.isNotBlank(normalized)) {
                    aggBool.must(QueryBuilders.multiMatchQuery(normalized,
                            "title", "description", "categories", "types", "brand", "tags",
                            "title.ngram", "description.ngram", "tags.ngram")
                            .type(MultiMatchQueryBuilder.Type.BEST_FIELDS)
                            .fuzziness(Fuzziness.AUTO));
                }

                SearchSourceBuilder aggSSB = new SearchSourceBuilder().query(aggBool).size(0);

                // Top-level filters
                for (String field : ElasticProductUtils.getTopLevelFilters()) {
                    aggSSB.aggregation(AggregationBuilders.terms(field + "_agg")
                            .field(field)
                            .size(100)
                            .missing("N/A"));
                }

                // Nested variant filters
                NestedAggregationBuilder variantsNestedAgg = AggregationBuilders.nested("variants_nested", "variants");
                for (String variantField : ElasticProductUtils.getNestedFilters()) {
                    variantsNestedAgg.subAggregation(AggregationBuilders.terms(variantField + "_agg")
                            .field("variants." + variantField)
                            .size(100)
                            .missing("N/A"));
                }
                aggSSB.aggregation(variantsNestedAgg);

                SearchRequest aggRequest = new SearchRequest(Constants.ELASTIC_PRODUCT_INDEX_NAME).source(aggSSB);
                SearchResponse aggResp = restHighLevelClient.search(aggRequest, RequestOptions.DEFAULT);

                // ---------- Parse aggregations ----------
                LinkedHashMap<String, LinkedHashSet<String>> filtersSet = new LinkedHashMap<>();

                // Top-level fields
                for (String field : ElasticProductUtils.getTopLevelFilters()) {
                    LinkedHashSet<String> values = new LinkedHashSet<>();
                    Terms agg = aggResp.getAggregations().get(field + "_agg");
                    if (agg != null) {
                        agg.getBuckets().forEach(bucket -> {
                            String key = bucket.getKeyAsString();
                            if (key != null && !key.isEmpty() && !"N/A".equalsIgnoreCase(key))
                                values.add(key);
                        });
                    }
                    filtersSet.put(field, values);
                }

                // Nested variant fields
                Nested variantsNested = aggResp.getAggregations().get("variants_nested");
                if (variantsNested != null) {
                    Map<String, String> variantAggMap = Map.of(
                            "color", "color_agg",
                            "size", "size_agg",
                            "materials", "materials_agg");
                    for (String field : ElasticProductUtils.getNestedFilters()) {
                        LinkedHashSet<String> values = new LinkedHashSet<>();
                        Terms agg = variantsNested.getAggregations().get(variantAggMap.get(field));
                        if (agg != null) {
                            agg.getBuckets().forEach(bucket -> {
                                String key = bucket.getKeyAsString();
                                if (key != null && !key.isEmpty() && !"N/A".equalsIgnoreCase(key))
                                    values.add(key);
                            });
                        }
                        filtersSet.put(field, values);
                    }
                }

                // Remove search term from filters
                if (StringUtils.isNotBlank(normalized)) {
                    String searchTermLower = normalized.toLowerCase(Locale.ROOT);
                    filtersSet.values().forEach(set -> set.removeIf(v -> v.equalsIgnoreCase(searchTermLower)));
                }

                Map<String, List<String>> filters = new LinkedHashMap<>();
                filtersSet.forEach((k, v) -> {
                    int idx = k.indexOf(".keyword");
                    String cleanKey = (idx != -1) ? k.substring(0, idx) : k;

                    if (!cleanKey.isEmpty()) {
                        // Capitalize first letter
                        cleanKey = Character.toUpperCase(cleanKey.charAt(0)) + cleanKey.substring(1);
                    }

                    filters.put(cleanKey, new ArrayList<>(v));
                });
                out.setFilters(filters);

                // Quick filters too
                Map<String, List<String>> quickFilters = new LinkedHashMap<>();
                for (String key : filtersSet.keySet()) {
                    String capKey = Character.toUpperCase(key.charAt(0)) + key.substring(1);
                    List<String> values = new ArrayList<>(filtersSet.get(key));
                    if (!values.isEmpty()) {
                        quickFilters.put(capKey, values.stream().limit(7).collect(Collectors.toList()));
                    }
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

        // Multi-match query on ngram fields
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.query(QueryBuilders.multiMatchQuery(inputLower,
                "title.ngram", "tags.ngram", "brand.ngram", "categories.ngram")
                .type(MultiMatchQueryBuilder.Type.PHRASE_PREFIX));
        sourceBuilder.size(100); // fetch more for proper ranking
        sourceBuilder.fetchSource(new String[] { "title", "tags", "brand", "categories" }, null);

        SearchRequest searchRequest = new SearchRequest(indexName);
        searchRequest.source(sourceBuilder);

        SearchResponse response = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);

        // Collect suggestions with field info
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
        }

        // Deduplicate while preserving first occurrence
        Map<String, SuggestionItem> uniqueMap = new LinkedHashMap<>();
        for (SuggestionItem item : candidates) {
            uniqueMap.putIfAbsent(item.text, item);
        }

        // Rank suggestions
        List<String> ranked = uniqueMap.values().stream()
                .sorted((a, b) -> Integer.compare(
                        getMatchScore(b.text.toLowerCase(Locale.ROOT), inputLower, b.field),
                        getMatchScore(a.text.toLowerCase(Locale.ROOT), inputLower, a.field)))
                .limit(size)
                .map(item -> item.text)
                .collect(Collectors.toList());

        return ranked;
    }

    // ---------------- Helper class for suggestions ----------------
    private static class SuggestionItem {
        String text;
        String field;

        SuggestionItem(String text, String field) {
            this.text = text;
            this.field = field;
        }
    }

    // ---------------- Improved scoring function ----------------
    private int getMatchScore(String text, String input, String field) {
        int baseScore;

        if (text.equals(input)) {
            baseScore = 100; // exact match
        } else if (text.startsWith(input)) {
            baseScore = 75; // starts with input
        } else if (text.contains(input)) {
            baseScore = 50 + input.length(); // contains input
        } else {
            // Partial word match
            String[] words = input.split("\\s+");
            int matchedWords = 0;
            for (String w : words) {
                if (text.contains(w))
                    matchedWords++;
            }
            baseScore = matchedWords * 10;
        }

        // Field boost
        switch (field) {
            case "title":
                return baseScore + 20;
            case "categories":
                return baseScore + 15;
            case "brand":
                return baseScore + 10;
            case "tags":
                return baseScore;
            default:
                return baseScore;
        }
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

}