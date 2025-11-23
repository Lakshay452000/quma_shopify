package com.quma.quma_shopify_backend.services;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import com.quma.quma_shopify_backend.enums.OrderPaymentStatus;
import com.quma.quma_shopify_backend.exceptions.ApiException;
import com.quma.quma_shopify_backend.models.dtos.CreateOrderRequestDTO;
import com.quma.quma_shopify_backend.models.dtos.InitiatePaymentResponseDTO;
import com.quma.quma_shopify_backend.models.dtos.PaymentDetailsRequestDTO;
import com.quma.quma_shopify_backend.models.dtos.PaymentRequestDTO;
import com.quma.quma_shopify_backend.models.mongo.Order;
import com.quma.quma_shopify_backend.repositories.mongo.OrderRepository;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.mongodb.core.query.Criteria;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class PaymentService {

    @Autowired
    OrderRepository orderRepository;

    @Autowired
    OrderService orderService;

    @Autowired
    RazorpayService razorpayService;

    @Autowired
    MongoTemplate mongoTemplate;

    @Value("${razorpay.key_id}")
    private String keyId;

    public InitiatePaymentResponseDTO startPayment(PaymentRequestDTO dto) {

        try {

            Order order;

            boolean isNewOrder = StringUtils.isBlank(dto.getOrderId());

            if (isNewOrder) {
                // Must have addressId to create a new order
                if (StringUtils.isBlank(dto.getAddressId())) {
                    throw new ApiException("Address ID is required for new order", 400);
                }

                order = orderService.createOrder(
                        new CreateOrderRequestDTO(dto.getCouponCode(), dto.getAddressId()));

            } else {
                order = orderRepository.findByOrderId(dto.getOrderId())
                        .orElseThrow(() -> new ApiException("Order not found", 404));
                if (!List.of(OrderPaymentStatus.PENDING, OrderPaymentStatus.CANCELLED, OrderPaymentStatus.FAILED)
                        .contains(order.getOrderPaymentStatus())
                        || order.getExpiresAt().isBefore(Instant.now())) {

                    throw new ApiException("Order is not in a payable state", 400);
                }

            }

            // Razorpay Order
            String receipt = order.getOrderId();

            com.razorpay.Order rpOrder = razorpayService.createOrder(
                    order.getAmount().longValue(),
                    receipt);

            // Save razorpay order details
            order.setRazorpayOrderId(rpOrder.get("id"));
            order.setReceipt(receipt);
            order.setOrderPaymentStatus(OrderPaymentStatus.PROCESSING);
            order.setPaymentStatusUpdatedAt(Instant.now());
            orderRepository.save(order);

            return buildResponse(order, rpOrder);

        } catch (Exception e) {
            log.error("Error while starting payment: {}", e.getMessage());
            throw new ApiException("Failed to start payment", 500);
        }
    }

    private InitiatePaymentResponseDTO buildResponse(Order order, com.razorpay.Order rpOrder) {
        InitiatePaymentResponseDTO res = new InitiatePaymentResponseDTO();
        res.setKey(keyId);
        res.setOrderId(order.getOrderId());
        res.setRazorpayOrderId(rpOrder.get("id"));
        res.setCurrency(rpOrder.get("currency"));
        res.setReceipt(rpOrder.get("receipt"));
        res.setStatus(rpOrder.get("status"));

        Number amountNumber = rpOrder.get("amount");
        res.setAmount(amountNumber.longValue() / 100);

        return res;
    }

    public void savePaymentDetails(PaymentDetailsRequestDTO payload) {

        if (payload == null || StringUtils.isBlank(payload.getPaymentId())) {
            return;
        }
        Query query = new Query();
        query.addCriteria(Criteria.where("orderId").is(payload.getOrderId()));
        query.addCriteria(Criteria.where("orderPaymentStatus").is(OrderPaymentStatus.PROCESSING));

        Update update = new Update()
                .set("paymentId", payload.getPaymentId())
                .set("signature", payload.getSignature())
                .set("paymentStatusUpdatedAt", Instant.now());

        // Only updates if order is still PROCESSING
        mongoTemplate.findAndModify(
                query,
                update,
                Order.class);

    }

    public String webhookPayment(String payload, String signature) {
        try {

            boolean isValid = razorpayService.verifyWebhookSignature(payload, signature);
            if (!isValid) {
                log.warn("Invalid webhook signature");
                throw new ApiException("Invalid signature", 500);
            }

            JSONObject json = new JSONObject(payload);
            String event = json.getString("event");

            // Extract base payment info
            JSONObject payment = json.getJSONObject("payload")
                    .getJSONObject("payment")
                    .getJSONObject("entity");

            String razorpayPaymentId = payment.getString("id");
            String razorpayOrderId = payment.getString("order_id");
            BigDecimal amount = payment.getBigDecimal("amount");
            String receipt = razorpayService.getReceiptFromRazorpayId(razorpayOrderId);

            if ("payment.captured".equals(event)) {

                // ATOMIC: Update only if current status is PROCESSING
                Query query = new Query()
                        .addCriteria(Criteria.where("orderId").is(receipt))
                        .addCriteria(Criteria.where("orderPaymentStatus").ne(OrderPaymentStatus.PAID));

                Update update = new Update()
                        .set("orderPaymentStatus", OrderPaymentStatus.PAID)
                        .set("paymentId", razorpayPaymentId)
                        .set("razorpayOrderId", razorpayOrderId)
                        .set("paymentStatusUpdatedAt", Instant.now());

                Order updatedOrder = mongoTemplate.findAndModify(query, update, Order.class);

                if (updatedOrder == null) {
                    // Status is NOT PROCESSING — check what happened
                    Order existing = orderRepository.findByOrderId(receipt)
                            .orElse(null);

                    if (existing != null && existing.getOrderPaymentStatus() == OrderPaymentStatus.PAID) {
                        // Already paid earlier → refund duplicate
                        razorpayService.refundPayment(razorpayPaymentId, amount.longValue());
                        log.warn("Duplicate payment refunded for {}", receipt);
                        return "Duplicate refunded";
                    }

                    log.warn("Webhook received but order not in PROCESSING state: {}", receipt);
                    return "Ignored – not in PROCESSING state";
                }

                log.info("Atomic update: Payment captured for {}", receipt);
                return "Payment captured";

            }

            else if ("payment.failed".equals(event)) {

                Query query = new Query()
                        .addCriteria(Criteria.where("orderId").is(receipt))
                        .addCriteria(Criteria.where("orderPaymentStatus").is(OrderPaymentStatus.PROCESSING));

                Update update = new Update()
                        .set("orderPaymentStatus", OrderPaymentStatus.FAILED)
                        .set("paymentStatusUpdatedAt", Instant.now());

                mongoTemplate.findAndModify(query, update, Order.class);

                log.info("Atomic update: Payment failed for {}", receipt);
                return "Payment failed";
            }

            return "Webhook processed";

        } catch (Exception e) {
            log.error("Webhook processing failed", e);
            throw new ApiException("Error while processing webhook", 500);
        }
    }

    public Map<String, Object> checkOrderPaymentStatus(String orderId) {
        Order order = orderRepository.findByOrderId(orderId).orElse(null);
        if (order == null) {
            throw new ApiException("Order not found", 404);
        }
        return Map.of("status", order.getOrderPaymentStatus().toString(),
                "paymentTime", order.getPaymentStatusUpdatedAt());
    }
}