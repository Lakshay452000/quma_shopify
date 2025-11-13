package com.quma.quma_shopify_backend.controller;

import com.quma.quma_shopify_backend.models.dtos.HomeAnalyticsResponseDTO;
import com.quma.quma_shopify_backend.models.dtos.ProductAnalyticRequest;
import com.quma.quma_shopify_backend.services.ProductAnalyticService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.util.Collections;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/product-analytics")
@RequiredArgsConstructor
public class ProductAnalyticController {

    private final ProductAnalyticService analyticService;

    @PostMapping("/event")
    public ResponseEntity<String> recordAnalyticsEvent(@Valid @RequestBody ProductAnalyticRequest request) {
        analyticService.recordEvents(Collections.singletonList(request));
        return ResponseEntity.ok("Event recorded successfully");
    }

    @GetMapping("/home")
    public ResponseEntity<HomeAnalyticsResponseDTO> getHomeAnalytics() throws Exception {
        return ResponseEntity.ok(analyticService.getHomeAnalytics());
    }
}
