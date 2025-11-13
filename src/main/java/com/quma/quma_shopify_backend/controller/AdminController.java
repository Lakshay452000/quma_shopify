package com.quma.quma_shopify_backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.quma.quma_shopify_backend.cron.ProductAnalyticCron;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final ProductAnalyticCron productAnalyticCron;

    @PostMapping("/analytics/refresh")
    public ResponseEntity<String> refreshAnalytics() {
        productAnalyticCron.runAnalyticsAggregation();
        return ResponseEntity.ok(" Analytics refresh done successfully");
    }
}
