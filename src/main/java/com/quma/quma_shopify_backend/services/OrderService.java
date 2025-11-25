package com.quma.quma_shopify_backend.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quma.quma_shopify_backend.enums.AnalyticsEventType;
import com.quma.quma_shopify_backend.enums.OrderPaymentStatus;
import com.quma.quma_shopify_backend.enums.OrderShipmentStatus;
import com.quma.quma_shopify_backend.enums.OrderStatus;
import com.quma.quma_shopify_backend.exceptions.ApiException;
import com.quma.quma_shopify_backend.models.dtos.CartItemDTO;
import com.quma.quma_shopify_backend.models.dtos.CouponApplyResponseDTO;
import com.quma.quma_shopify_backend.models.dtos.CreateOrderRequestDTO;
import com.quma.quma_shopify_backend.models.dtos.OrderResponseDTO;
import com.quma.quma_shopify_backend.models.dtos.PagedOrderResponseDTO;
import com.quma.quma_shopify_backend.models.dtos.ProductAnalyticRequest;
import com.quma.quma_shopify_backend.models.dtos.ShippingAddressDTO;
import com.quma.quma_shopify_backend.models.mongo.Address;
import com.quma.quma_shopify_backend.models.mongo.CartItemMongoDTO;
import com.quma.quma_shopify_backend.models.mongo.Order;
import com.quma.quma_shopify_backend.models.mongo.OrderItemDTO;
import com.quma.quma_shopify_backend.repositories.mongo.AddressRepository;
import com.quma.quma_shopify_backend.repositories.mongo.CartRepository;
import com.quma.quma_shopify_backend.repositories.mongo.OrderRepository;
import com.quma.quma_shopify_backend.utilities.UserContext;
import com.quma.quma_shopify_backend.utilities.UtilityFunctions;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.FindAndModifyOptions;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import com.quma.quma_shopify_backend.models.mongo.Product;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;

    private final CartRepository cartRepository;

    private final ObjectMapper objectMapper;

    private final CouponService couponService;

    private final ShippingService shippingService;

    private final AddressRepository addressRepository;

    private final ProductAnalyticService analyticService;

    private final MongoTemplate mongoTemplate;

    private final ProductStockService productStockService;

    /**
     * Create an order for the current user from their cart.
     */
    @Transactional
    public Order createOrder(CreateOrderRequestDTO createOrderRequestDTO) {
        String couponCode = createOrderRequestDTO.getCouponCode();
        String addressId = createOrderRequestDTO.getAddressId();
        String username = UserContext.get().getUsername();

        if (username == null || username.isEmpty()) {
            throw new ApiException("Unauthenticated user", 401);
        }

        CartItemMongoDTO cart = cartRepository.findByUsername(username);
        if (cart == null || CollectionUtils.isEmpty(cart.getCartItemDTOs())) {
            throw new ApiException("Cart is empty. Cannot create order.", 400);
        }

        if (StringUtils.isBlank(addressId)) {
            throw new ApiException("Shipping address is required to create order", 400);
        }

        if (addressRepository.findByAddressId(addressId) == null) {
            throw new ApiException("Invalid shipping address", 400);
        }

        // -------------------------------
        // 1) ATOMIC STOCK VALIDATION + REDUCTION
        // -------------------------------
        for (CartItemDTO ci : cart.getCartItemDTOs()) {
            productStockService.reduceStock(ci.getProductId(), ci.getIdentifier(), ci.getQuantity());
        }

        // -------------------------------
        // 2) CALCULATE ORDER AMOUNT
        // -------------------------------
        BigDecimal totalAmount = cart.getCartItemDTOs().stream()
                .map(ci -> ci.getPrice().multiply(BigDecimal.valueOf(ci.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Order order = new Order();

        order.setSubTotal(totalAmount);
        order.setDiscount(BigDecimal.ZERO);
        order.setAmount(totalAmount);

        if (couponCode != null && !couponCode.isEmpty()) {
            CouponApplyResponseDTO coupon = couponService.applyCoupon(couponCode, totalAmount);
            order.setCouponCode(couponCode);
            order.setSubTotal(coupon.getOriginalAmount());
            order.setDiscount(coupon.getDiscountAmount());
            totalAmount = coupon.getFinalAmount();
            order.setAmount(totalAmount);
        }

        order.setShippingFee(shippingService.calculateShipping("Shiprocket", "ADDR-123"));
        BigDecimal totalAmountAfterShipping = order.getAmount().add(order.getShippingFee());
        order.setAmount(totalAmountAfterShipping);
        // -------------------------------
        // 3) COPY CART ITEMS INTO ORDER
        // -------------------------------
        List<OrderItemDTO> orderItems = cart.getCartItemDTOs().stream()
                .map(ci -> objectMapper.convertValue(ci, OrderItemDTO.class))
                .collect(Collectors.toList());

        // -------------------------------
        // 4) CREATE ORDER
        // -------------------------------
        order.setOrderId("ORD-" + UtilityFunctions.getRandomId());
        order.setUsername(username);
        order.setItems(orderItems);
        order.setCurrency("INR");
        order.setOrderPaymentStatus(OrderPaymentStatus.PENDING);
        order.setOrderShipmentStatus(OrderShipmentStatus.PENDING);
        order.setOrderStatus(OrderStatus.ACTIVE);
        order.setAddressId(addressId);
        order.setCreatedAt(Instant.now());
        order.setExpiresAt(Instant.now().plus(24, ChronoUnit.HOURS));

        orderRepository.save(order);

        // -------------------------------
        // 5) ANALYTICS (ASYNC)
        // -------------------------------
        List<ProductAnalyticRequest> analyticsEvents = cart.getCartItemDTOs().stream().map(item -> {
            ProductAnalyticRequest ar = new ProductAnalyticRequest();
            ar.setUsername(username);
            ar.setProductId(item.getProductId());
            ar.setIdentifier(item.getIdentifier());
            ar.setImageUrl(item.getImageUrl());
            ar.setEventType(AnalyticsEventType.ORDERED);
            ar.setCategories(item.getCategories());
            return ar;
        }).toList();

        if (!analyticsEvents.isEmpty()) {
            analyticService.recordEvents(analyticsEvents);
        }

        // -------------------------------
        // 6) CLEAR CART
        // -------------------------------
        cartRepository.delete(cart);

        return order;
    }

    public OrderResponseDTO getOrderById(String orderId) {
        String username = UserContext.get().getUsername();
        if (username == null || username.isEmpty()) {
            throw new ApiException("Unauthenticated user", 401);
        }

        Order order = orderRepository.findByOrderIdAndUsername(orderId, username);
        if (order == null) {
            throw new ApiException("Order not found", 404);
        }
        Address address = addressRepository.findByAddressId(order.getAddressId());
        if (address == null) {
            throw new ApiException("Shipping address not found", 404);
        }

        ShippingAddressDTO shippingAddress = objectMapper.convertValue(address, ShippingAddressDTO.class);

        OrderResponseDTO orderResponseDTO = objectMapper.convertValue(order, OrderResponseDTO.class);
        orderResponseDTO.setShippingAddress(shippingAddress);
        return orderResponseDTO;
    }

    public PagedOrderResponseDTO getAllOrders(int page, int size, OrderStatus status) {
        String username = UserContext.get().getUsername();
        if (username == null || username.isEmpty()) {
            throw new ApiException("Unauthenticated user", 401);
        }

        if (size <= 0)
            size = 5;
        if (page < 0)
            page = 0;

        // always sort by createdAt descending
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Order> ordersPage;

        // if status is null → fetch all
        if (status == null) {
            ordersPage = orderRepository.findByUsername(username, pageRequest);
        } else {
            ordersPage = orderRepository.findByUsernameAndOrderStatus(username, status, pageRequest);
        }

        if (ordersPage.isEmpty()) {
            return null;
        }

        List<OrderResponseDTO> orderDTOs = ordersPage.stream()
                .map(order -> objectMapper.convertValue(order, OrderResponseDTO.class))
                .toList();

        PagedOrderResponseDTO response = new PagedOrderResponseDTO();
        response.setOrders(orderDTOs);
        response.setPage(page);
        response.setSize(size);
        response.setTotalElements(ordersPage.getTotalElements());
        response.setTotalPages(ordersPage.getTotalPages());

        return response;
    }

    public boolean hasUserPurchasedProduct(String username, String productId) {
        return orderRepository.existsByUsernameAndItemsProductId(username, productId);
    }

}
