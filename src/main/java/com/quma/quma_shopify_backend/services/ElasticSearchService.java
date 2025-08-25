package com.quma.quma_shopify_backend.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quma.quma_shopify_backend.enums.SortType;
import com.quma.quma_shopify_backend.exceptions.ApiException;
import com.quma.quma_shopify_backend.models.dtos.ProductsRequestDTO;
import com.quma.quma_shopify_backend.models.elastic.ProductElasticDocument;
import com.quma.quma_shopify_backend.models.elastic.ProductElasticResponseDocument;
import com.quma.quma_shopify_backend.services.implementations.RedisStore;
import com.quma.quma_shopify_backend.utilities.Constants;
import com.quma.quma_shopify_backend.utilities.ElasticProductFilters;

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
import org.elasticsearch.search.aggregations.bucket.nested.Nested;
import org.elasticsearch.search.aggregations.bucket.nested.NestedAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedTerms;
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
import java.util.stream.Stream;

@Service
@Slf4j
@SuppressWarnings("deprecation")
public class ElasticSearchService {

    @Autowired
    private RestHighLevelClient restHighLevelClient;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RedisStore redisStore;

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

            // ---------- Base query ----------
            BoolQueryBuilder bool = QueryBuilders.boolQuery()
                    .filter(QueryBuilders.termQuery("isActive", true));

            if (StringUtils.isNotBlank(normalized)) {
                bool.must(QueryBuilders.multiMatchQuery(normalized,
                        "title", "description", "category", "brand", "tags",
                        "title.ngram", "description.ngram", "tags.ngram")
                        .type(MultiMatchQueryBuilder.Type.BEST_FIELDS)
                        .fuzziness(Fuzziness.AUTO));
            }

            // ---------- Build search source ----------
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

            boolean isFirstPage = (request.getSortValues() == null || request.getSortValues().length == 0);

            // ---------- Aggregations for first page ----------
            if (isFirstPage) {

                // Top-level
                for (String field : ElasticProductFilters.getTopLevelFilters()) {
                    ssb.aggregation(AggregationBuilders.terms(field + "_agg")
                            .field(field)
                            .size(100)
                            .missing("N/A"));
                }

                // Nested variants
                NestedAggregationBuilder variantsNestedAgg = AggregationBuilders.nested("variants_nested", "variants");
                for (String variantField : ElasticProductFilters.getNestedFilters()) {
                    variantsNestedAgg.subAggregation(AggregationBuilders.terms(variantField + "_agg")
                            .field("variants." + variantField)
                            .size(100)
                            .missing("N/A"));
                }
                ssb.aggregation(variantsNestedAgg);
            }

            // ---------- Execute search ----------
            SearchRequest searchRequest = new SearchRequest(Constants.ELASTIC_PRODUCT_INDEX_NAME).source(ssb);
            SearchResponse resp = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);

            // ---------- Parse hits ----------
            List<ProductElasticDocument> products = new ArrayList<>();
            Object[] lastSortValues = null;
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

            // ---------- Parse aggregations ----------
            if (isFirstPage) {
                LinkedHashMap<String, LinkedHashSet<String>> filtersSet = new LinkedHashMap<>();

                // Top-level
                for (String field : ElasticProductFilters.getTopLevelFilters()) {
                    LinkedHashSet<String> values = new LinkedHashSet<>();
                    Terms agg = resp.getAggregations().get(field + "_agg");
                    if (agg != null) {
                        agg.getBuckets().forEach(bucket -> {
                            String key = bucket.getKeyAsString();
                            if (key != null && !key.isEmpty() && !"N/A".equalsIgnoreCase(key))
                                values.add(key);
                        });
                    }
                    String responseKey = field.endsWith(".keyword") ? field.substring(0, field.indexOf(".keyword"))
                            : field;
                    filtersSet.put(responseKey, values);
                }

                // Nested variant fields
                Nested variantsNested = resp.getAggregations().get("variants_nested");
                if (variantsNested != null) {
                    Map<String, String> variantAggMap = Map.of(
                            "color", "color_agg",
                            "size", "size_agg",
                            "material", "material_agg");
                    for (String field : ElasticProductFilters.getNestedFilters()) {
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

                // Convert Set -> List preserving order
                Map<String, List<String>> filters = new LinkedHashMap<>();
                filtersSet.forEach((k, v) -> filters.put(k, new ArrayList<>(v)));
                out.setFilters(filters);

                // Top 7 quick filters preserving the same order
                List<String> quickFilters = new ArrayList<>();
                for (String key : filtersSet.keySet()) {
                    List<String> list = new ArrayList<>(filtersSet.get(key));
                    for (String v : list) {
                        if (quickFilters.size() < 7)
                            quickFilters.add(v);
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
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.size(size);
        sourceBuilder.fetchSource(new String[] { "title", "tags" }, null);

        MultiMatchQueryBuilder multiMatch = QueryBuilders.multiMatchQuery(input)
                .field("title.autocomplete")
                .field("tags.ngram")
                .type(MultiMatchQueryBuilder.Type.BOOL_PREFIX);

        sourceBuilder.query(multiMatch);

        SearchRequest request = new SearchRequest(indexName);
        request.source(sourceBuilder);

        SearchResponse response = restHighLevelClient.search(request, RequestOptions.DEFAULT);

        return Arrays.stream(response.getHits().getHits())
                .flatMap(hit -> {
                    String title = (String) hit.getSourceAsMap().get("title");

                    Object tagsObj = hit.getSourceAsMap().get("tags");
                    List<String> tags = null;
                    if (tagsObj instanceof List<?>) {
                        tags = ((List<?>) tagsObj).stream()
                                .map(Object::toString)
                                .collect(Collectors.toList());
                    }

                    return tags == null
                            ? Stream.of(title)
                            : Stream.concat(Stream.of(title), tags.stream());
                })
                .filter(s -> s != null && !s.isEmpty())
                .distinct()
                .limit(size)
                .collect(Collectors.toList());
    }

}
