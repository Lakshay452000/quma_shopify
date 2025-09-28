package com.quma.quma_shopify_backend.controller;

import com.quma.quma_shopify_backend.models.dtos.OrderRequestDTO;
import com.quma.quma_shopify_backend.models.dtos.OrderResponseDTO;
import com.quma.quma_shopify_backend.services.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @PostMapping("create-order")
    public ResponseEntity<OrderResponseDTO> createOrder(@RequestBody OrderRequestDTO request) throws Exception {
        return ResponseEntity.ok(orderService.createOrder(request));
    }
}
