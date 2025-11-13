package com.quma.quma_shopify_backend.controller;

import com.quma.quma_shopify_backend.models.dtos.ElasticVariantsResponseDTO;
import com.quma.quma_shopify_backend.models.dtos.ProductRequestDTO;
import com.quma.quma_shopify_backend.models.dtos.ProductSearchRequestDTO;
import com.quma.quma_shopify_backend.models.elastic.ProductElasticDocument;
import com.quma.quma_shopify_backend.models.elastic.ProductElasticResponseDocument;
import com.quma.quma_shopify_backend.services.ElasticSearchService;
import com.quma.quma_shopify_backend.utilities.Constants;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
            @RequestBody ProductSearchRequestDTO productSearchRequestDTO) throws Exception {
        return ResponseEntity.ok(elasticSearchService.searchProducts(productSearchRequestDTO));
    }

    @GetMapping("/suggest")
    public List<String> getSuggestions(
            @RequestParam String input,
            @RequestParam(defaultValue = "10") int size) throws IOException {
        return elasticSearchService.getSuggestions(Constants.ELASTIC_PRODUCT_INDEX_NAME, input, size);
    }

    @GetMapping("/get-variants/{baseProductId}")
    public ResponseEntity<List<ElasticVariantsResponseDTO>> getProductVariants(
            @PathVariable String baseProductId) throws Exception {
        return ResponseEntity.ok(elasticSearchService.getVariantsByBaseProductId(baseProductId));
    }

    @PostMapping("/batch")
    public ResponseEntity<List<ProductElasticDocument>> searchProductsByIds(
            @RequestBody ProductRequestDTO productsRequestDTO) throws Exception {
        return ResponseEntity.ok(elasticSearchService.getProductsByIds(productsRequestDTO.getProductIds()));
    }
}