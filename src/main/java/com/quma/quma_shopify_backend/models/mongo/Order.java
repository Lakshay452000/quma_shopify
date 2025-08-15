package com.quma.quma_shopify_backend.models.mongo;

import com.quma.quma_shopify_backend.enums.OrderStatus;
import com.quma.quma_shopify_backend.models.dtos.OrderItemDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Document(collection = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    @Id
    private String id;
    private String userId;
    private List<OrderItemDTO> items;
    private OrderStatus status;
}
