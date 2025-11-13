package com.quma.quma_shopify_backend.models.dtos;

import java.util.List;
import lombok.Data;

@Data
public class PagedOrderResponseDTO {
    private List<OrderResponseDTO> orders;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
}
