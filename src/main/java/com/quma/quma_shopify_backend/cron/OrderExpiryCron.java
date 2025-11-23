package com.quma.quma_shopify_backend.cron;

import com.quma.quma_shopify_backend.repositories.mongo.OrderRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderExpiryCron {

    private final OrderRepository orderRepository;

    // Runs every hour at minute 0
    @Scheduled(cron = "0 0 * * * *")
    public void expireOrders() {
        orderRepository.expireOrders();
    }

    @Scheduled(fixedRate = 1000 * 60 * 3)
    public void resetProcessingOrders() {
        orderRepository.resetProcessingOrders();
    }
}
