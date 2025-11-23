package com.quma.quma_shopify_backend.repositories.mongo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import com.quma.quma_shopify_backend.enums.OrderPaymentStatus;
import com.quma.quma_shopify_backend.models.dtos.RazorpayPaymentDTO;
import com.quma.quma_shopify_backend.models.mongo.Order;
import com.quma.quma_shopify_backend.models.mongo.OrderItemDTO;
import com.quma.quma_shopify_backend.services.ProductStockService;
import com.quma.quma_shopify_backend.services.RazorpayService;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@RequiredArgsConstructor
@Slf4j
public class OrderRepositoryCustomImpl implements OrderRepositoryCustom {

        private final MongoTemplate mongoTemplate;
        private final ProductStockService productStockService;
        private final RazorpayService razorpayService;

        @Override
        public long expireOrders() {

                // 1) Find all ACTIVE + PENDING orders that expired
                Query query = new Query(
                                Criteria.where("orderStatus").is("ACTIVE")
                                                .and("orderPaymentStatus").is("PENDING")
                                                .and("expiresAt").lt(Instant.now()));

                List<Order> expiredOrders = mongoTemplate.find(query, Order.class);

                if (expiredOrders.isEmpty()) {
                        return 0;
                }

                // 2) For each expired order → restore stock
                for (Order order : expiredOrders) {
                        for (OrderItemDTO item : order.getItems()) {
                                productStockService.addStock(
                                                item.getProductId(),
                                                item.getIdentifier(),
                                                item.getQuantity());
                        }

                        // 3) Mark order as EXPIRED
                        Query updateQuery = new Query(Criteria.where("orderId").is(order.getOrderId()));
                        Update update = new Update().set("orderStatus", "EXPIRED")
                                        .currentTimestamp("updatedAt");

                        mongoTemplate.updateFirst(updateQuery, update, Order.class);
                }

                return expiredOrders.size();
        }

        @Override
        public long resetProcessingOrders() {
                Instant now = Instant.now();
                Instant threshold = now.minus(3, ChronoUnit.MINUTES);

                // Find orders that are still PROCESSING and whose paymentStatusUpdatedAt is
                // older than 3 minutes
                Query findQuery = new Query(
                                Criteria.where("orderPaymentStatus").is(OrderPaymentStatus.PROCESSING)
                                                .and("paymentStatusUpdatedAt").lt(threshold));

                List<Order> candidates = mongoTemplate.find(findQuery, Order.class);
                if (CollectionUtils.isEmpty(candidates)) {
                        return 0L;
                }

                long processed = 0L;

                for (Order order : candidates) {
                        try {
                                // 1) Try to determine a paymentId
                                String paymentId = order.getPaymentId();
                                if (StringUtils.isBlank(paymentId)
                                                && StringUtils.isNotBlank(order.getRazorpayOrderId())) {
                                        // Assumes razorpayService can list or return the payment id(s) for a given
                                        // razorpay order id.
                                        // Implement this in your RazorpayService if not present:
                                        // Optional<String> maybePaymentId =
                                        // razorpayService.findPaymentIdByOrderId(order.getRazorpayOrderId());
                                        // paymentId = maybePaymentId.orElse(null);
                                        paymentId = razorpayService.findPaymentIdByOrderId(order.getRazorpayOrderId()); // returns
                                                                                                                        // null
                                                                                                                        // if
                                                                                                                        // none
                                }

                                // If we still have no paymentId → mark PENDING (no payment attempt recorded)
                                // and continue
                                if (StringUtils.isBlank(paymentId)) {
                                        Query q = new Query(Criteria.where("orderId").is(order.getOrderId())
                                                        .and("orderPaymentStatus").is(OrderPaymentStatus.PROCESSING));
                                        Update u = new Update()
                                                        .set("orderPaymentStatus", OrderPaymentStatus.PENDING)
                                                        .set("paymentStatusUpdatedAt", Instant.now());
                                        Order updated = mongoTemplate.findAndModify(q, u,
                                                        FindAndModifyOptions.options().returnNew(true), Order.class);
                                        if (updated != null) {
                                                processed++;
                                        }
                                        continue;
                                }

                                // 2) Fetch payment details from Razorpay
                                // Assumed razorpayService.fetchPayment(paymentId) returns an object describing
                                // the payment.
                                // Example: a DTO with getStatus() returning
                                // "created"/"authorized"/"captured"/"failed".
                                RazorpayPaymentDTO rpPayment = razorpayService.fetchPayment(paymentId);
                                if (rpPayment == null) {
                                        // nothing found on Razorpay → set PENDING so cron/webhook can handle later
                                        Query q = new Query(Criteria.where("orderId").is(order.getOrderId())
                                                        .and("orderPaymentStatus").is(OrderPaymentStatus.PROCESSING));
                                        Update u = new Update()
                                                        .set("orderPaymentStatus", OrderPaymentStatus.PENDING)
                                                        .set("paymentStatusUpdatedAt", Instant.now());
                                        Order updated = mongoTemplate.findAndModify(q, u,
                                                        FindAndModifyOptions.options().returnNew(true), Order.class);
                                        if (updated != null)
                                                processed++;
                                        continue;
                                }

                                String rpStatus = rpPayment.getStatus(); // "created", "authorized", "captured",
                                                                         // "failed", etc.

                                // Prepare atomic query that only updates if order is still PROCESSING
                                Query atomicQuery = new Query(Criteria.where("orderId").is(order.getOrderId())
                                                .and("orderPaymentStatus").is(OrderPaymentStatus.PROCESSING));

                                if ("captured".equalsIgnoreCase(rpStatus)) {
                                        Update update = new Update()
                                                        .set("orderPaymentStatus", OrderPaymentStatus.PAID)
                                                        .set("paymentId", paymentId)
                                                        .set("razorpayOrderId", order.getRazorpayOrderId())
                                                        .set("paymentStatusUpdatedAt", Instant.now());
                                        Order updated = mongoTemplate.findAndModify(atomicQuery, update,
                                                        FindAndModifyOptions.options().returnNew(true), Order.class);
                                        if (updated != null)
                                                processed++;
                                } else if ("failed".equalsIgnoreCase(rpStatus)) {
                                        Update update = new Update()
                                                        .set("orderPaymentStatus", OrderPaymentStatus.FAILED)
                                                        .set("paymentStatusUpdatedAt", Instant.now());
                                        Order updated = mongoTemplate.findAndModify(atomicQuery, update,
                                                        FindAndModifyOptions.options().returnNew(true), Order.class);
                                        if (updated != null)
                                                processed++;
                                } else if ("authorized".equalsIgnoreCase(rpStatus)) {
                                        // Try to capture. Need amount in paise for capture API.
                                        try {
                                                long amountPaise = order.getAmount().multiply(BigDecimal.valueOf(100))
                                                                .longValue();
                                                // Assumes razorpayService.capturePayment returns true on success, false
                                                // on failure
                                                boolean captureSuccess = razorpayService.capturePayment(paymentId,
                                                                amountPaise);

                                                if (captureSuccess) {
                                                        Update update = new Update()
                                                                        .set("orderPaymentStatus",
                                                                                        OrderPaymentStatus.PAID)
                                                                        .set("paymentId", paymentId)
                                                                        .set("paymentStatusUpdatedAt", Instant.now());
                                                        Order updated = mongoTemplate.findAndModify(atomicQuery, update,
                                                                        FindAndModifyOptions.options().returnNew(true),
                                                                        Order.class);
                                                        if (updated != null)
                                                                processed++;
                                                } else {
                                                        Update update = new Update()
                                                                        .set("orderPaymentStatus",
                                                                                        OrderPaymentStatus.FAILED)
                                                                        .set("paymentStatusUpdatedAt", Instant.now());
                                                        Order updated = mongoTemplate.findAndModify(atomicQuery, update,
                                                                        FindAndModifyOptions.options().returnNew(true),
                                                                        Order.class);
                                                        if (updated != null)
                                                                processed++;
                                                }
                                        } catch (Exception captureEx) {
                                                // Capture failed — treat as FAILED (but log the exception)
                                                log.error("Error while capturing payment {} for order {}: {}",
                                                                paymentId, order.getOrderId(), captureEx.getMessage(),
                                                                captureEx);
                                                Update update = new Update()
                                                                .set("orderPaymentStatus", OrderPaymentStatus.FAILED)
                                                                .set("paymentStatusUpdatedAt", Instant.now());
                                                Order updated = mongoTemplate.findAndModify(atomicQuery, update,
                                                                FindAndModifyOptions.options().returnNew(true),
                                                                Order.class);
                                                if (updated != null)
                                                        processed++;
                                        }
                                } else {
                                        // status == "created" or other non-final state → mark PENDING (no confirmed
                                        // payment)
                                        Update update = new Update()
                                                        .set("orderPaymentStatus", OrderPaymentStatus.PENDING)
                                                        .set("paymentStatusUpdatedAt", Instant.now());
                                        Order updated = mongoTemplate.findAndModify(atomicQuery, update,
                                                        FindAndModifyOptions.options().returnNew(true), Order.class);
                                        if (updated != null)
                                                processed++;
                                }

                        } catch (Exception ex) {
                                // Log and continue. Do not throw — cron should continue processing other
                                // orders.
                                log.error("Error while resetting processing order {}: {}", order.getOrderId(),
                                                ex.getMessage(), ex);
                        }
                }

                return processed;
        }

}
