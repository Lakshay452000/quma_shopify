package com.quma.quma_shopify_backend.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quma.quma_shopify_backend.enums.AnalyticsEventType;
import com.quma.quma_shopify_backend.exceptions.ApiException;
import com.quma.quma_shopify_backend.models.dtos.ProductAnalyticRequest;
import com.quma.quma_shopify_backend.models.dtos.ProductResponseMongoDTO;
import com.quma.quma_shopify_backend.models.dtos.ProductSaveRequestDTO;
import com.quma.quma_shopify_backend.models.mongo.Product;
import com.quma.quma_shopify_backend.repositories.mongo.ProductRepository;
import com.quma.quma_shopify_backend.utilities.UserContext;
import com.quma.quma_shopify_backend.utilities.UtilityFunctions;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class ProductService {

    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private ElasticSearchService elasticSearchService;
    @Autowired
    private ProductAnalyticService productAnalyticService;

    @Autowired
    private ObjectMapper objectMapper;

    public void saveProduct(ProductSaveRequestDTO product) {
        try {
            Product productToSave = objectMapper.convertValue(product, Product.class);
            productToSave.setProductId("PROD-" + UtilityFunctions.getRandomId());

            Product savedProduct = productRepository.save(productToSave);
            elasticSearchService.indexProducts(Collections.singletonList(savedProduct.getProductId()));

        } catch (Exception e) {
            log.error("Failed to save product: {}", e.getMessage());
            throw new ApiException("Product registration failed", 500);
        }
    }

    public List<ProductResponseMongoDTO> getProductList(List<String> productIds) {
        try {
            List<Product> products = productRepository.findAllByProductIdIn(productIds);
            List<ProductResponseMongoDTO> productResponseMongoDTOs = objectMapper.convertValue(
                    products, new TypeReference<>() {
                    });

            // 🔹 Prepare view events asynchronously
            List<ProductAnalyticRequest> events = products.stream()
                    .map(product -> {
                        String imageUrl = Optional.ofNullable(product.getVariants())
                                .filter(v -> !v.isEmpty())
                                .map(v -> v.get(0).getImages())
                                .filter(imgs -> !imgs.isEmpty())
                                .map(imgs -> imgs.get(0))
                                .orElse(null);

                        return ProductAnalyticRequest.builder()
                                .productId(product.getProductId())
                                .identifier(product.getVariants().get(0).getIdentifier())
                                .categories(product.getCategories())
                                .imageUrl(imageUrl)
                                .eventType(AnalyticsEventType.VIEW)
                                .username(UserContext.get().getUsername())
                                .build();
                    })
                    .toList();

            productAnalyticService.recordEvents(events);

            return productResponseMongoDTOs;
        } catch (Exception e) {
            log.error("Failed to fetch products list: {}", e.getMessage(), e);
            throw e;
        }
    }

}
