package com.quma.quma_shopify_backend.models.dtos;

import java.time.LocalDateTime;

public record ReviewResponseDTO(
        String name,
        String username,
        int rating,
        String comment,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
