package com.quma.quma_shopify_backend.services;

import com.quma.quma_shopify_backend.enums.OrderStatus;
import com.quma.quma_shopify_backend.exceptions.ApiException;
import com.quma.quma_shopify_backend.models.dtos.OrderRequestDTO;
import com.quma.quma_shopify_backend.models.dtos.OrderResponseDTO;
import com.quma.quma_shopify_backend.models.dtos.PaymentInitResponse;
import com.quma.quma_shopify_backend.models.dtos.PaymentVerificationDTO;
import com.quma.quma_shopify_backend.models.mongo.Order;
import com.quma.quma_shopify_backend.repositories.mongo.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private PaymentService paymentService;
    @Autowired// Razorpay logic
    private DeliveryService deliveryService; // Shiprocket logic

    public OrderResponseDTO createOrder(OrderRequestDTO request) {
        // 1. Save pending order in DB
        Order order = new Order();
        order.setUserId(request.getUserId());
        order.setItems(request.getItems());
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order = orderRepository.save(order);

        // 2. Initiate payment
        PaymentInitResponse paymentInit = paymentService.initiatePayment(order);

        // 3. Return payment link/order details
        return new OrderResponseDTO(order, paymentInit.getPaymentUrl());
    }

    public void handlePaymentWebhook(String payload) {
        // Verify and parse Razorpay payload
        PaymentVerificationDTO result = paymentService.verifyPayment(payload);

        if (result.isSuccess()) {
            // Update order status to PAID
            Order order = orderRepository.findById(result.getOrderId())
                    .orElseThrow(() -> new ApiException("Order not found", 404));
            order.setStatus(OrderStatus.PAID);
            orderRepository.save(order);

            // Initiate delivery
            deliveryService.createShipment(order);
        }
    }

    public OrderResponseDTO getOrder(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ApiException("Order not found", 404));
        return new OrderResponseDTO(order, null);
    }

    public void cancelOrder(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ApiException("Order not found", 404));
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        if (order.getStatus() == OrderStatus.PAID) {
            paymentService.refundPayment(order);
        }
    }

    public List<OrderResponseDTO> getOrdersByUser(String userId) {
        return orderRepository.findByUserId(userId)
                .stream()
                .map(order -> new OrderResponseDTO(order, null))
                .toList();
    }
}

