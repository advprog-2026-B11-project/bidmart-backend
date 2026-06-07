package com.example.bidmart.order.service;

import com.example.bidmart.order.model.Order;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface OrderService {
    Order createOrderAutomatically(UUID listingId, UUID buyerId, UUID sellerId, BigDecimal amount);
    List<Order> getOrdersByUser(UUID userId);
    Order updateOrderStatus(UUID orderId, String newStatusStr);
    Order updateTrackingNumber(UUID orderId, UUID requesterId, String trackingNumber);
    Order confirmDelivery(UUID orderId, UUID requesterId);
    Order disputeOrder(UUID orderId, UUID requesterId, String reason);
    Order resolveDispute(UUID orderId, boolean refundBuyer);
    void deleteOrder(UUID orderId);
}
