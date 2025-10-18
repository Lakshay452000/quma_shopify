package com.quma.quma_shopify_backend.models.dtos;

import jakarta.validation.constraints.*;

public record ReviewRequestDTO(
                @NotNull String productId,
                @Min(1) @Max(5) int rating,
                String comment) {
}
