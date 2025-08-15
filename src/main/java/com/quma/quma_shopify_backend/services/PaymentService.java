package com.quma.quma_shopify_backend.services;

import com.quma.quma_shopify_backend.models.dtos.PaymentInitResponse;
import com.quma.quma_shopify_backend.models.dtos.PaymentVerificationDTO;
import com.quma.quma_shopify_backend.models.mongo.Order;
import org.springframework.stereotype.Service;

@Service
public class PaymentService {

    public PaymentInitResponse initiatePayment(Order order) {
        // TODO: Replace with actual Razorpay API call
        return new PaymentInitResponse("https://dummy-payment.com/pay/" + order.getId());
    }

    public PaymentVerificationDTO verifyPayment(String payload) {
        // TODO: Verify Razorpay signature
        return new PaymentVerificationDTO(true, "orderIdFromPayload");
    }

    public void refundPayment(Order order) {
        // TODO: Call Razorpay refund API
    }
}

