package com.quma.quma_shopify_backend.services;

import com.quma.quma_shopify_backend.models.mongo.ProductModel;
import com.quma.quma_shopify_backend.repositories.HomeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class HomeService {

    @Autowired
    HomeRepository homeRepository;

    public ProductModel saveSampleProduct() {
        ProductModel productModel = new ProductModel();
        productModel.setTitle("Welcome to Quma Shopify Sample Product");
        return homeRepository.save(productModel);
    }
}
