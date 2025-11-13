package com.quma.quma_shopify_backend.services;

import java.math.BigDecimal;
import java.util.Map;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.quma.quma_shopify_backend.enums.OrderPaymentStatus;
import com.quma.quma_shopify_backend.exceptions.ApiException;
import com.quma.quma_shopify_backend.models.dtos.InitiatePaymentResponseDTO;
import com.quma.quma_shopify_backend.models.mongo.Order;
import com.quma.quma_shopify_backend.repositories.mongo.OrderRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class PaymentService {

    @Autowired
    OrderRepository orderRepository;

    @Autowired
    RazorpayService razorpayService;

    @Value("${razorpay.key_id}")
    private String keyId;

    public InitiatePaymentResponseDTO startPayment(String orderId) {
        try {
            Order entity = orderRepository.findByOrderId(orderId)
                    .orElseThrow(() -> new ApiException("Order not found", 404));

            String receipt = entity.getOrderId();

            com.razorpay.Order razorpayOrder = razorpayService.createOrder(entity.getAmount().longValue(), receipt);

            entity.setRazorpayOrderId(razorpayOrder.get("id"));
            entity.setReceipt(receipt);
            orderRepository.save(entity);

            // create DTO to return
            InitiatePaymentResponseDTO response = new InitiatePaymentResponseDTO();
            response.setKey(keyId);
            response.setOrderId(entity.getId());
            response.setRazorpayOrderId(razorpayOrder.get("id"));
            Number amountNumber = razorpayOrder.get("amount");
            response.setAmount(amountNumber.longValue() / 100);
            response.setCurrency(razorpayOrder.get("currency"));
            response.setReceipt(razorpayOrder.get("receipt"));
            response.setStatus(razorpayOrder.get("status"));

            return response;
        } catch (Exception e) {
            log.error("Error while starting payment", e.getMessage());
            throw new ApiException(e.getMessage(), 500);
        }
    }

    // public Map<String, String> verifyPayment(VerifyPaymentDTO payload) throws
    // Exception {

    // Order order = orderRepository.findByOrderId(payload.getOrderId());
    // if (order == null)
    // new ApiException("Order not found", 404);
    // boolean valid = razorpayService.verifySignature(
    // payload.getRazorpayOrderId(),
    // payload.getPaymentId(),
    // payload.getSignature());
    // if (valid) {
    // order.setPaymentId(payload.getPaymentId());
    // order.setSignature(payload.getSignature());
    // order.setOrderPaymentStatus(OrderPaymentStatus.PAID)
    // orderRepository.save(order);
    // return Map.of("status", "success");
    // } else {
    // order.setStatus("FAILED");
    // orderRepository.save(order);
    // return Map.of("status", "invalid_signature");
    // }
    // }

    public String webhookPayment(String payload, String signature) {
        try {

            boolean isValid = razorpayService.verifyWebhookSignature(payload, signature);
            if (!isValid) {
                log.warn("Invalid webhook signature");
                throw new ApiException("Invalid signature", 500);
            }

            // Parse event
            JSONObject json = new JSONObject(payload);
            String event = json.getString("event");

            if ("payment.captured".equals(event)) {
                JSONObject payment = json.getJSONObject("payload")
                        .getJSONObject("payment")
                        .getJSONObject("entity");

                String razorpayPaymentId = payment.getString("id");
                String razorpayOrderId = payment.getString("order_id");
                BigDecimal amount = payment.getBigDecimal("amount");
                String receipt = razorpayService.getReceiptFromRazorpayId(razorpayOrderId);

                // Find order by receipt (orderId)
                Order order = orderRepository.findByOrderId(receipt);
                if (order == null) {
                    log.warn("Order not found for receipt {}", receipt);
                    return "Order not found";
                }

                if (OrderPaymentStatus.PAID.equals(order.getOrderPaymentStatus())) {
                    // Duplicate payment → refund
                    try {
                        razorpayService.refundPayment(razorpayPaymentId, amount.longValue());
                        log.warn("Duplicate payment detected for order {}, refund initiated", receipt);
                        return "Duplicate payment refunded";
                    } catch (Exception e) {
                        log.error("Error initiating refund for duplicate payment: {}", e.getMessage());
                        return "Duplicate payment refund failed";
                    }
                }

                order.setOrderPaymentStatus(OrderPaymentStatus.PAID);
                order.setPaymentId(razorpayPaymentId);
                order.setRazorpayOrderId(razorpayOrderId);
                order.setAmount(amount);
                orderRepository.save(order);

                log.info("Payment captured for order {}", receipt);

            } else if ("payment.failed".equals(event)) {
                JSONObject payment = json.getJSONObject("payload")
                        .getJSONObject("payment")
                        .getJSONObject("entity");

                String razorpayOrderId = payment.getString("order_id");
                String receipt = razorpayService.getReceiptFromRazorpayId(razorpayOrderId);

                Order order = orderRepository.findById(receipt).orElse(null);
                if (order != null) {
                    order.setOrderPaymentStatus(OrderPaymentStatus.FAILED);
                    orderRepository.save(order);
                }
                log.info("Payment failed for order {}", receipt);
            }

            return "Webhook processed";

        } catch (Exception e) {
            log.error("Webhook processing failed", e);
            throw new ApiException("Error while processing webhook", 500);
        }
    }

    public Map<String, String> checkOrderPaymentStatus(String orderId) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) {
            throw new ApiException("Order not found", 404);
        }
        return Map.of("status", order.getOrderPaymentStatus().toString());
    }
}