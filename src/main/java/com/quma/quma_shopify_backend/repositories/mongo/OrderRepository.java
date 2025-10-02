package com.quma.quma_shopify_backend.repositories.mongo;

import com.quma.quma_shopify_backend.enums.OrderStatus;
import com.quma.quma_shopify_backend.models.mongo.Order;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends MongoRepository<Order, String> {
    List<Order> findByUsername(String username);

    Order findByRazorpayOrderId(String razorpayOrderId);

    Order findByOrderIdAndUsername(String razorpayOrderId, String username);

    Order findByOrderId(String razorpayOrderId, String username);

    Page<Order> findByUsername(String username, Pageable pageable);

    Page<Order> findByUsernameAndOrderStatus(String username, OrderStatus orderStatus, Pageable pageable);
}
