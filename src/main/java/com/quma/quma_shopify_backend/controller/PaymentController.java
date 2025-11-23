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
import com.quma.quma_shopify_backend.models.dtos.PaymentDetailsRequestDTO;
import com.quma.quma_shopify_backend.models.dtos.PaymentRequestDTO;
import com.quma.quma_shopify_backend.services.PaymentService;

@RestController
@RequestMapping("/payment")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @PostMapping("/start")
    public ResponseEntity<InitiatePaymentResponseDTO> startPayment(@RequestBody PaymentRequestDTO paymentRequestDTO)
            throws Exception {
        return ResponseEntity.ok(paymentService.startPayment(paymentRequestDTO));
    }

    @PostMapping("/save-details")
    public ResponseEntity<Void> savePaymentDetails(
            @RequestBody PaymentDetailsRequestDTO paymentDetailsRequestDTO)
            throws Exception {
        paymentService.savePaymentDetails(paymentDetailsRequestDTO);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> webhookPayment(@RequestBody String payload,
            @RequestHeader("X-Razorpay-Signature") String signature) {
        return ResponseEntity.ok(paymentService.webhookPayment(payload, signature));
    }

    @GetMapping("/status/{orderId}")
    public ResponseEntity<Map<String, Object>> checkOrderPaymentStatus(@PathVariable String orderId) {
        return ResponseEntity.ok(paymentService.checkOrderPaymentStatus(orderId));
    }
}