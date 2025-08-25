package com.quma.quma_shopify_backend.controller;

import com.quma.quma_shopify_backend.models.dtos.ProductsRequestDTO;
import com.quma.quma_shopify_backend.models.elastic.ProductElasticResponseDocument;
import com.quma.quma_shopify_backend.services.ElasticSearchService;
import com.quma.quma_shopify_backend.utilities.Constants;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/search")
public class SearchController {

    @Autowired
    private ElasticSearchService elasticSearchService;

    @PostMapping("/all-products")
    public ResponseEntity<ProductElasticResponseDocument> searchProducts(
            @RequestBody ProductsRequestDTO productsRequestDTO) throws Exception {
        return ResponseEntity.ok(elasticSearchService.searchProducts(productsRequestDTO));
    }

    @GetMapping("/suggest")
    public List<String> getSuggestions(
            @RequestParam String input,
            @RequestParam(defaultValue = "10") int size) throws IOException {
        return elasticSearchService.getSuggestions(Constants.ELASTIC_PRODUCT_INDEX_NAME, input, size);
    }

}