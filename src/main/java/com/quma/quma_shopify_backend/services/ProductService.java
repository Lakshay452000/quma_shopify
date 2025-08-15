package com.quma.quma_shopify_backend.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quma.quma_shopify_backend.exceptions.ApiException;
import com.quma.quma_shopify_backend.models.dtos.ProductsRequestDTO;
import com.quma.quma_shopify_backend.models.elastic.ProductElasticDocument;
import com.quma.quma_shopify_backend.models.mongo.Product;
import com.quma.quma_shopify_backend.repositories.mongo.ProductRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

    private List<ProductElasticDocument> getProductElasticDocuments(Product product) {
        return product.getVariants().stream()
                .map(variant -> {
                    ProductElasticDocument doc = new ProductElasticDocument();
                    doc.setId(product.getId() + "_" + variant.getIdentifier()); // Unique per variant
                    doc.setOwnerId(product.getOwnerId());
                    doc.setTitle(product.getTitle());
                    doc.setCategoryId(product.getCategoryId());
                    doc.setDescription(product.getDescription());
                    doc.setPrice(variant.getPrice());
                    doc.setDiscountedPrice(variant.getDiscountedPrice());
                    doc.setTags(product.getTags());
                    // Choose image — prefer variant-specific if available
                    if (product.getImages() != null && !product.getImages().isEmpty()) {
                        doc.setImageUrl(product.getImages().get(0));
                    } else {
                        doc.setImageUrl(null);
                    }
                    doc.setAverageRating(product.getAverageRating());
                    doc.setTotalReviews(product.getTotalReviews() != null ? product.getTotalReviews().intValue() : null);
                    doc.setCreatedAt(product.getCreatedAt());
                    doc.setUpdatedAt(product.getUpdatedAt());

                    return doc;
                })
                .toList();
    }

    public void saveProduct(Product product) {
        try {
            Product savedProduct = productRepository.save(product);
            List<ProductElasticDocument> productElasticDocuments = getProductElasticDocuments(savedProduct);
            elasticSearchService.indexProducts(productElasticDocuments);

        } catch (Exception e) {
            log.error("Failed to save product: {}", e.getMessage());
            throw new ApiException("Product registration failed", 500);
        }
    }

    public List<ProductElasticDocument> searchProducts(ProductsRequestDTO productsRequestDTO) throws Exception {
        return elasticSearchService.searchProducts(productsRequestDTO.getProductFilters(),
                productsRequestDTO.getPageSize(),
                productsRequestDTO.getLastProductId(),
                productsRequestDTO.getSortBy(),
                productsRequestDTO.getSortType());
    }

    public List<Product> getProductList(List<String> productIds) {
        return productRepository.findAllByIdIn(productIds);
    }
}

