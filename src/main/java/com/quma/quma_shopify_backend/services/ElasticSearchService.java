package com.quma.quma_shopify_backend.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quma.quma_shopify_backend.enums.SortType;
import com.quma.quma_shopify_backend.models.dtos.ProductsRequestDTO;
import com.quma.quma_shopify_backend.models.elastic.ProductElasticDocument;
import com.quma.quma_shopify_backend.models.elastic.ProductElasticResponseDocument;
import com.quma.quma_shopify_backend.utilities.Constants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
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
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@SuppressWarnings("deprecation")
public class ElasticSearchService {

    @Autowired
    private RestHighLevelClient restHighLevelClient;

    @Autowired
    private ObjectMapper objectMapper;

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

    public ProductElasticResponseDocument searchProducts(ProductsRequestDTO request) throws IOException {

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder()
                .size(request.getPageSize())
                .sort(request.getSortBy(),
                        SortType.DESC.equals(request.getSortType()) ? SortOrder.DESC : SortOrder.ASC)
                .sort("_id", SortType.DESC.equals(request.getSortType()) ? SortOrder.DESC : SortOrder.ASC);
        // Apply filters if provided
        if (MapUtils.isNotEmpty(request.getProductFilters())) {
            BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
            for (Map.Entry<String, Object> entry : request.getProductFilters().entrySet()) {
                // Use termQuery for exact match
                boolQuery.must(QueryBuilders.termQuery(entry.getKey(), entry.getValue()));
            }
            searchSourceBuilder.query(boolQuery);
        }
        if (request.getSortValues() != null &&
                request.getSortValues().length == 2) { // must match number of sort fields
            searchSourceBuilder.searchAfter(request.getSortValues());
        }

        SearchRequest searchRequest = new SearchRequest(Constants.ELASTIC_PRODUCT_INDEX_NAME)
                .source(searchSourceBuilder);

        SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);

        List<ProductElasticDocument> products = new ArrayList<>();
        Object[] lastSortValues = null;
        for (SearchHit hit : searchResponse.getHits().getHits()) {
                ProductElasticDocument product = objectMapper.convertValue(hit.getSourceAsMap(), ProductElasticDocument.class);
                product.setId(hit.getId());
                lastSortValues = hit.getSortValues();
                products.add(product);
        }
        ProductElasticResponseDocument elasticResponseDocument = new ProductElasticResponseDocument();
        elasticResponseDocument.setProducts(products);
        elasticResponseDocument.setSortValues(lastSortValues);
        return elasticResponseDocument;
    }
}
