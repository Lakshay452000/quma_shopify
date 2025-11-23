package com.quma.quma_shopify_backend.controller;

import com.quma.quma_shopify_backend.enums.OrderStatus;
import com.quma.quma_shopify_backend.models.dtos.CreateOrderRequestDTO;
import com.quma.quma_shopify_backend.models.dtos.OrderResponseDTO;
import com.quma.quma_shopify_backend.models.dtos.PagedOrderResponseDTO;
import com.quma.quma_shopify_backend.services.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    // @PostMapping("/create-order")
    // public ResponseEntity<OrderResponseDTO> createOrder(@RequestBody
    // CreateOrderRequestDTO createOrderRequestDTO)
    // throws Exception {
    // return ResponseEntity.ok(orderService.createOrder(createOrderRequestDTO));
    // }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponseDTO> getOrderById(@PathVariable String orderId) throws Exception {
        return ResponseEntity.ok(orderService.getOrderById(orderId));
    }

    @GetMapping("/get-orders")
    public ResponseEntity<PagedOrderResponseDTO> getAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(required = false) OrderStatus status) {

        return ResponseEntity.ok(orderService.getAllOrders(page, size, status));
    }

}