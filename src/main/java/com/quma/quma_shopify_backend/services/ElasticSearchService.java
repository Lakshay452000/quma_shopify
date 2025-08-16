package com.quma.quma_shopify_backend.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quma.quma_shopify_backend.enums.SortType;
import com.quma.quma_shopify_backend.models.dtos.ProductsRequestDTO;
import com.quma.quma_shopify_backend.models.elastic.ProductElasticDocument;
import com.quma.quma_shopify_backend.utilities.Constants;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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

    public List<ProductElasticDocument> searchProducts(ProductsRequestDTO request) throws IOException {

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder()
                .size(request.getPageSize())
                .sort(request.getSortBy(),
                        SortType.DESC.equals(request.getSortType()) ? SortOrder.DESC : SortOrder.ASC)
                .sort("_id", SortType.DESC.equals(request.getSortType()) ? SortOrder.DESC : SortOrder.ASC);

        if (request.getLastSortValues() != null &&
                request.getLastSortValues().length == 2) { // must match number of sort fields
            searchSourceBuilder.searchAfter(request.getLastSortValues());
        }

        SearchRequest searchRequest = new SearchRequest(Constants.ELASTIC_PRODUCT_INDEX_NAME)
                .source(searchSourceBuilder);

        SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);

        List<ProductElasticDocument> products = new ArrayList<>();
        for (SearchHit hit : searchResponse.getHits().getHits()) {
            ProductElasticDocument product = objectMapper.convertValue(hit.getSourceAsMap(), ProductElasticDocument.class);
            product.setId(hit.getId());
            // Store sort values so frontend can send them back
            product.setSortValues(hit.getSortValues());
            products.add(product);
        }

        return products;
    }

}
