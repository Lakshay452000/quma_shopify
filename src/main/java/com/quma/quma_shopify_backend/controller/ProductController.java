package com.quma.quma_shopify_backend.controller;

import com.quma.quma_shopify_backend.models.dtos.ProductsRequestDTO;
import com.quma.quma_shopify_backend.models.elastic.ProductElasticDocument;
import com.quma.quma_shopify_backend.models.mongo.Product;
import com.quma.quma_shopify_backend.services.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/products")
public class ProductController {

    @Autowired
    private ProductService productService;

    @PostMapping("/save")
    public ResponseEntity<String> registerProduct(@RequestBody Product product) {
        try {
            productService.saveProduct(product);
            return ResponseEntity.ok("Product saved and indexed successfully.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error while saving product. Product was not added");
        }
    }

    @PostMapping("/list")
    public ResponseEntity<?> getProductsList(@RequestBody ProductsRequestDTO productsRequestDTO) {
        try {
            return ResponseEntity.ok(productService.getProductList(productsRequestDTO.getProductIds()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Internal server error while fetching products list");
        }
    }

    @PostMapping("/search/batch")
    public ResponseEntity<List<ProductElasticDocument>> searchProductsByIds(@RequestBody ProductsRequestDTO productsRequestDTO) throws Exception {
        return ResponseEntity.ok(productService.searchProductsByIds(productsRequestDTO.getProductIds()));
    }
}
