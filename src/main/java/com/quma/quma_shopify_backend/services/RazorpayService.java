package com.quma.quma_shopify_backend.services;

import java.nio.charset.StandardCharsets;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Hex;
import org.json.JSONObject;

import com.razorpay.Order;
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

    public Order createOrder(long amountInRupees, String receipt) throws Exception {
        long amountInPaise = amountInRupees * 100L;

        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", amountInPaise);
        orderRequest.put("currency", "INR");
        orderRequest.put("receipt", receipt);
        orderRequest.put("payment_capture", 1);

        return client.orders.create(orderRequest);
    }

    public boolean verifySignature(String orderId, String paymentId, String signature)
            throws Exception {
        JSONObject options = new JSONObject();
        options.put("razorpay_order_id", orderId);
        options.put("razorpay_payment_id", paymentId);
        options.put("razorpay_signature", signature);
        return Utils.verifyPaymentSignature(options, keySecret);
    }

    public boolean verifyWebhookSignature(String payload, String expectedSignature) {
        try {
            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secret_key = new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256_HMAC.init(secret_key);
            byte[] hash = sha256_HMAC.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String actualSignature = Hex.encodeHexString(hash);
            return actualSignature.equals(expectedSignature);
        } catch (Exception e) {
            throw new RuntimeException("Error verifying webhook signature", e);
        }
    }

    public Refund refundPayment(String paymentId, long amount) throws Exception {
        // amount should be in paise
        JSONObject refundRequest = new JSONObject();
        refundRequest.put("amount", amount); // optional, refund full if omitted

        // Use client.Payments.refund method
        return client.payments.refund(paymentId, refundRequest);
    }

    public String getReceiptFromRazorpayId(String razorpayOrderId) throws Exception {
        Order razorpayOrder = client.orders.fetch(razorpayOrderId);
        String receipt = razorpayOrder.get("receipt");
        return receipt;
    }
}
