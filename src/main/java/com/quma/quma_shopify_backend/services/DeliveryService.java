package com.quma.quma_shopify_backend.services;

import com.quma.quma_shopify_backend.models.mongo.Order;
import org.springframework.stereotype.Service;

@Service
public class DeliveryService {

    public void createShipment(Order order) {
        // TODO: Call Shiprocket API to schedule shipment
        System.out.println("Shipment created for order: " + order.getId());
    }
}
