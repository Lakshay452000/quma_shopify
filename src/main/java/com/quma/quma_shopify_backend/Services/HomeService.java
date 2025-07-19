package com.quma.quma_shopify_backend.Services;

import com.quma.quma_shopify_backend.Models.ProductModel;
import com.quma.quma_shopify_backend.Repositories.HomeRepository;
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
