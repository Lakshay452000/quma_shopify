package com.quma.quma_shopify_backend.services;

import com.quma.quma_shopify_backend.exceptions.ApiException;
import com.quma.quma_shopify_backend.models.dtos.OrderRequestDTO;
import com.quma.quma_shopify_backend.models.dtos.OrderResponseDTO;
import com.quma.quma_shopify_backend.models.mongo.Order;
import com.quma.quma_shopify_backend.models.mongo.OrderItemDTO;
import com.quma.quma_shopify_backend.repositories.mongo.OrderRepository;
import com.quma.quma_shopify_backend.utilities.UtilityFunctions;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    public OrderResponseDTO createOrder(OrderRequestDTO request) throws Exception {
        try {
            if (CollectionUtils.isEmpty(request.getItems())) {
                throw new ApiException("Please add some items in cart", 403);
            }
            List<OrderItemDTO> items = new ArrayList<>();
            long totalAmountPaise = 0;

            for (OrderItemDTO itemDto : request.getItems()) {
                OrderItemDTO orderItem = new OrderItemDTO();
                orderItem.setProductId(itemDto.getProductId());
                orderItem.setQuantity(itemDto.getQuantity());
                orderItem.setPrice(itemDto.getPrice());

                items.add(orderItem);
                totalAmountPaise += itemDto.getPrice() * itemDto.getQuantity();
            }

            Order newOrder = new Order();
            newOrder.setUserId(request.getUserId());
            newOrder.setItems(items);
            newOrder.setAmount(totalAmountPaise);
            newOrder.setCurrency("INR");
            String orderId = "ORDER-" + UtilityFunctions.getRandomId();
            newOrder.setOrderId(orderId);
            orderRepository.save(newOrder);

            return new OrderResponseDTO(newOrder.getId(), newOrder.getAmount(), newOrder.getOrderPaymentStatus(),
                    newOrder.getOrderShipmentStatus(), newOrder.getOrderStatus());
        } catch (Exception e) {
            throw new ApiException("Error creating order: " + e.getMessage(), 500);
        }
    }
}
