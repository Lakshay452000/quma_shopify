package com.quma.quma_shopify_backend.controller;

import com.quma.quma_shopify_backend.models.dtos.ProductRequestDTO;
import com.quma.quma_shopify_backend.models.dtos.ProductSaveRequestDTO;
import com.quma.quma_shopify_backend.services.ProductService;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/products")
public class ProductController {

    @Autowired
    private ProductService productService;

    @PostMapping("/save")
    public ResponseEntity<String> registerProduct(@Valid @RequestBody ProductSaveRequestDTO productSaveRequestDTO) {
        try {
            productService.saveProduct(productSaveRequestDTO);
            return ResponseEntity.ok("Product saved and indexed successfully.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error while saving product. Product was not added");
        }
    }

    @PostMapping("/list")
    public ResponseEntity<?> getProductsList(@RequestBody ProductRequestDTO productsRequestDTO) {
        try {
            return ResponseEntity.ok(productService.getProductList(productsRequestDTO.getProductIds()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Internal server error while fetching products list");
        }
    }

}
