package com.quma.quma_shopify_backend.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quma.quma_shopify_backend.enums.SortType;
import com.quma.quma_shopify_backend.models.elastic.ProductElasticDocument;
import com.quma.quma_shopify_backend.utilities.Constants;
import io.micrometer.common.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
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

import java.util.ArrayList;
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

    public List<ProductElasticDocument> searchProducts(Map<String, Object> filters,
                                                       int pageSize, String lastProductId,
                                                       String sortBy, SortType sortType) throws Exception {
        try {
            SearchRequest searchRequest = new SearchRequest(Constants.ELASTIC_PRODUCT_INDEX_NAME);

            BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
            for (Map.Entry<String, Object> entry : filters.entrySet()) {
                Object value = entry.getValue();

                if (value instanceof Number || value instanceof Boolean) {
                    boolQuery.must(QueryBuilders.termQuery(entry.getKey(), value));
                } else {
                    boolQuery.must(QueryBuilders.matchQuery(entry.getKey(), value.toString()));
                }
            }
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder()
                    .query(boolQuery)
                    .size(pageSize)
                    .sort(sortBy, SortType.DESC.equals(sortType) ? SortOrder.DESC : SortOrder.ASC);

            // Add search_after only if it's not the first page
            if (StringUtils.isNotBlank(lastProductId)) {
                searchSourceBuilder.searchAfter(new Object[]{lastProductId});
            }
            searchRequest.source(searchSourceBuilder);
            SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
            List<ProductElasticDocument> results = new ArrayList<>();
            for (SearchHit hit : searchResponse.getHits().getHits()) {
                results.add(objectMapper.readValue(hit.getSourceAsString(), ProductElasticDocument.class));
            }
            return results;
        } catch (Exception e) {
            log.error("Error searching products in Elastic with search_after {}", e.getMessage());
            throw e;
        }
    }
}
