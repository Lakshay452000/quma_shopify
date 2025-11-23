package com.quma.quma_shopify_backend.services;

import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Hex;
import org.json.JSONArray;
import org.json.JSONObject;

import com.quma.quma_shopify_backend.models.dtos.RazorpayPaymentDTO;
import com.razorpay.Order;
import com.razorpay.Payment;
import com.razorpay.RazorpayClient;
import com.razorpay.Refund;
import com.razorpay.Utils;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class RazorpayService {

    @Value("${razorpay.key_id}")
    private String keyId;

    @Value("${razorpay.key_secret}")
    private String keySecret;

    @Value("${razorpay.webhook_secret}")
    private String webhookSecret;

    private RazorpayClient client;

    @PostConstruct
    public void init() throws Exception {
        this.client = new RazorpayClient(keyId, keySecret);
    }

    // -----------------------------
    // CREATE ORDER
    // -----------------------------
    public Order createOrder(long amountInRupees, String receipt) throws Exception {
        long amountInPaise = amountInRupees * 100;

        JSONObject req = new JSONObject();
        req.put("amount", amountInPaise);
        req.put("currency", "INR");
        req.put("receipt", receipt);
        req.put("payment_capture", 1);

        return client.orders.create(req);
    }

    // -----------------------------
    // VERIFY SIGNATURE
    // -----------------------------
    public boolean verifySignature(String orderId, String paymentId, String signature) throws Exception {
        JSONObject options = new JSONObject();
        options.put("razorpay_order_id", orderId);
        options.put("razorpay_payment_id", paymentId);
        options.put("razorpay_signature", signature);

        return Utils.verifyPaymentSignature(options, keySecret);
    }

    // -----------------------------
    // VERIFY WEBHOOK SIGNATURE
    // -----------------------------
    public boolean verifyWebhookSignature(String payload, String expectedSignature) {
        try {
            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secret_key = new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256_HMAC.init(secret_key);
            byte[] hash = sha256_HMAC.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String actual = Hex.encodeHexString(hash);
            return actual.equals(expectedSignature);
        } catch (Exception e) {
            throw new RuntimeException("Webhook signature error", e);
        }
    }

    // -----------------------------
    // 1) FIND PAYMENT ID BY ORDER ID
    // -----------------------------
    public String findPaymentIdByOrderId(String razorpayOrderId) {
        try {
            JSONObject criteria = new JSONObject();
            criteria.put("order_id", razorpayOrderId);

            // Razorpay Java SDK returns List<Payment>
            List<Payment> list = client.payments.fetchAll(criteria);

            if (list == null || list.isEmpty()) {
                return null;
            }

            // Take first payment
            Payment p = list.get(0);

            return p.get("id"); // correct way to read data

        } catch (Exception e) {
            return null;
        }
    }

    // -----------------------------
    // 2) FETCH PAYMENT DETAILS
    // -----------------------------
    public RazorpayPaymentDTO fetchPayment(String paymentId) {
        try {
            Payment p = client.payments.fetch(paymentId);

            RazorpayPaymentDTO dto = new RazorpayPaymentDTO();

            dto.setId(p.get("id"));
            dto.setStatus(p.get("status")); // created/authorized/captured/failed
            dto.setOrderId(p.get("order_id"));
            dto.setAmount(p.get("amount"));
            dto.setMethod(p.get("method"));

            return dto;
        } catch (Exception e) {
            return null;
        }
    }

    // -----------------------------
    // 3) CAPTURE PAYMENT
    // -----------------------------
    public boolean capturePayment(String paymentId, long amountPaise) {
        try {
            JSONObject req = new JSONObject();
            req.put("amount", amountPaise);

            Payment captured = client.payments.capture(paymentId, req);

            return "captured".equalsIgnoreCase(captured.get("status"));
        } catch (Exception e) {
            return false;
        }
    }

    // -----------------------------
    // 4) REFUND PAYMENT (ALREADY CORRECT)
    // -----------------------------
    public Refund refundPayment(String paymentId, long amountPaise) throws Exception {
        JSONObject req = new JSONObject();
        req.put("amount", amountPaise);
        return client.payments.refund(paymentId, req);
    }

    // -----------------------------
    // 5) FETCH RECEIPT FROM ORDER
    // -----------------------------
    public String getReceiptFromRazorpayId(String razorpayOrderId) throws Exception {
        Order order = client.orders.fetch(razorpayOrderId);
        return order.get("receipt");
    }
}
