package com.quma.quma_shopify_backend.models.dtos;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class HomeAnalyticsResponseDTO {
    // Each metric (views, favorites, etc.) — directly from Redis
    private Map<String, List<Map<String, Object>>> productSections;

    // Top 10 categories (from cron)
    private List<Map<String, Object>> topCategories;
}
