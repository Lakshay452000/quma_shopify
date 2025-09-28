package com.quma.quma_shopify_backend.models.mongo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import com.quma.quma_shopify_backend.enums.OrderPaymentStatus;
import com.quma.quma_shopify_backend.enums.OrderShipmentStatus;
import com.quma.quma_shopify_backend.enums.OrderStatus;

import java.time.Instant;
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

    private Long amount;
    private String currency;
    private String orderId;
    private String razorpayOrderId;
    private String paymentId;
    private String signature;
    private String receipt;
    private OrderPaymentStatus orderPaymentStatus = OrderPaymentStatus.PENDING;
    private OrderShipmentStatus orderShipmentStatus = OrderShipmentStatus.PENDING;
    private OrderStatus orderStatus = OrderStatus.ACTIVE;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
