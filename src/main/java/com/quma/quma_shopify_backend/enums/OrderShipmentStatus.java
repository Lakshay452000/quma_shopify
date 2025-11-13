package com.quma.quma_shopify_backend.enums;

public enum OrderShipmentStatus {
    PENDING, // Shipment not yet created
    PACKED, // Items packed, ready for pickup
    SHIPPED, // Handed over to courier
    IN_TRANSIT, // On the way to customer
    OUT_FOR_DELIVERY, // With delivery agent
    DELIVERED, // Delivered successfully
    RETURN_REQUESTED, // Customer requested return
    RETURNED, // Returned to seller
    LOST, // Shipment lost in transit
    CANCELLED; // Shipment cancelled
}
