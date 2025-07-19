package com.quma.quma_shopify_backend.Controller;

import com.quma.quma_shopify_backend.Models.ProductModel;
import com.quma.quma_shopify_backend.Services.HomeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/home")
public class HomeController {

    @Autowired
    HomeService homeService;

    @GetMapping("/index")
    public ProductModel index() {
        return homeService.saveSampleProduct();
    }
}
