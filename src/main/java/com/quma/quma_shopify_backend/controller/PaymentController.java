package com.quma.quma_shopify_backend.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.quma.quma_shopify_backend.models.dtos.InitiatePaymentResponseDTO;
import com.quma.quma_shopify_backend.models.dtos.VerifyPaymentDTO;
import com.quma.quma_shopify_backend.services.PaymentService;

@RestController
@RequestMapping("/payment")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @PostMapping("/start/{orderId}")
    public ResponseEntity<InitiatePaymentResponseDTO> startPayment(@PathVariable String orderId) {
        return ResponseEntity.ok(paymentService.startPayment(orderId));
    }

    // @PostMapping("/verify")
    // public ResponseEntity<Map<String, String>> verifyPayment(@RequestBody
    // VerifyPaymentDTO verifyPaymentDTO)
    // throws Exception {
    // return ResponseEntity.ok(paymentService.verifyPayment(verifyPaymentDTO));
    // }

    @PostMapping("/webhook")
    public ResponseEntity<String> webhookPayment(@RequestBody String payload,
            @RequestHeader("X-Razorpay-Signature") String signature) {
        return ResponseEntity.ok(paymentService.webhookPayment(payload, signature));
    }

    @GetMapping("/status/{orderId}")
    public ResponseEntity<Map<String, String>> checkOrderPaymentStatus(@PathVariable String orderId) {
        return ResponseEntity.ok(paymentService.checkOrderPaymentStatus(orderId));
    }
}