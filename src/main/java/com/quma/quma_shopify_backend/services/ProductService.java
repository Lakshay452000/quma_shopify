package com.quma.quma_shopify_backend.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quma.quma_shopify_backend.exceptions.ApiException;
import com.quma.quma_shopify_backend.models.dtos.ProductsRequestDTO;
import com.quma.quma_shopify_backend.models.elastic.ProductElasticDocument;
import com.quma.quma_shopify_backend.models.elastic.ProductElasticResponseDocument;
import com.quma.quma_shopify_backend.models.mongo.Product;
import com.quma.quma_shopify_backend.repositories.mongo.ProductRepository;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@Slf4j
public class ProductService {

    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private ElasticSearchService elasticSearchService;

    @Autowired
    private ObjectMapper objectMapper;

    private ProductElasticDocument getProductElasticDocuments(Product product) {
        return objectMapper.convertValue(product, ProductElasticDocument.class);
    }

    public void saveProduct(Product product) {
        try {
            Product savedProduct = productRepository.save(product);
            ProductElasticDocument productElasticDocuments = getProductElasticDocuments(savedProduct);
            elasticSearchService.indexProducts(Collections.singletonList(productElasticDocuments));

        } catch (Exception e) {
            log.error("Failed to save product: {}", e.getMessage());
            throw new ApiException("Product registration failed", 500);
        }
    }

    public ProductElasticResponseDocument searchProducts(ProductsRequestDTO productsRequestDTO) throws Exception {
        return elasticSearchService.searchProducts(productsRequestDTO);
    }

    public List<ProductElasticDocument> searchProductsByIds(List<String> productIds) throws Exception {
        return elasticSearchService.getProductsByIds(productIds);
    }

    public List<Product> getProductList(List<String> productIds) {
        try {
            List<ObjectId> productObjectIds = productIds.stream()
                    .map(ObjectId::new)
                    .toList();
            return productRepository.findAllByIdIn(productObjectIds);
        } catch (Exception e) {
            log.error("Failed to fetch products list: {}", e.getMessage());
            throw e;
        }
    }
}

