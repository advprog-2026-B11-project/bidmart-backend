package com.example.bidmart.order.service;

import com.example.bidmart.bidding.exception.ResourceNotFoundException;
import com.example.bidmart.common.event.OrderDeliveredEvent;
import com.example.bidmart.common.event.OrderRefundedEvent;
import com.example.bidmart.order.exception.InvalidOrderStatusTransitionException;
import com.example.bidmart.order.model.Order;
import com.example.bidmart.order.model.OrderStatus;
import com.example.bidmart.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Order createOrderAutomatically(UUID listingId, UUID buyerId, UUID sellerId, BigDecimal amount) {
        Optional<Order> existingOrder = orderRepository.findByListingId(listingId);
        if (existingOrder.isPresent()) {
            return existingOrder.get();
        }

        Order newOrder = Order.builder()
                .listingId(listingId)
                .buyerId(buyerId)
                .sellerId(sellerId)
                .amount(amount)
                .status(OrderStatus.CREATED)
                .createdAt(LocalDateTime.now())
                .build();
        return orderRepository.save(newOrder);
    }

    public List<Order> getOrdersByBuyer(UUID buyerId) {
        return orderRepository.findByBuyerId(buyerId);
    }

    @Transactional
    public Order updateOrderStatus(UUID orderId, String newStatusStr) {
        Order order = getOrderOrThrow(orderId);
        OrderStatus newStatus = OrderStatus.fromString(newStatusStr);
        
        if (!order.getStatus().canTransitionTo(newStatus)) {
            throw new InvalidOrderStatusTransitionException(order.getStatus().name(), newStatus.name());
        }
        
        order.setStatus(newStatus);
        return orderRepository.save(order);
    }

    @Transactional
    public Order updateTrackingNumber(UUID orderId, UUID requesterId, String trackingNumber) {
        Order order = getOrderOrThrow(orderId);

        if (!order.getSellerId().equals(requesterId)) {
            throw new IllegalArgumentException("Hanya penjual yang dapat memperbarui nomor resi.");
        }
        
        if (!order.getStatus().canTransitionTo(OrderStatus.SHIPPED)) {
            throw new InvalidOrderStatusTransitionException(order.getStatus().name(), OrderStatus.SHIPPED.name());
        }
        
        order.setTrackingNumber(trackingNumber);
        order.setStatus(OrderStatus.SHIPPED);
        return orderRepository.save(order);
    }

    @Transactional
    public Order confirmDelivery(UUID orderId, UUID requesterId) {
        Order order = getOrderOrThrow(orderId);

        if (!order.getBuyerId().equals(requesterId)) {
            throw new IllegalArgumentException("Hanya pembeli yang dapat mengonfirmasi penerimaan barang.");
        }
        
        if (!order.getStatus().canTransitionTo(OrderStatus.DELIVERED)) {
            throw new InvalidOrderStatusTransitionException(order.getStatus().name(), OrderStatus.DELIVERED.name());
        }

        order.setStatus(OrderStatus.DELIVERED);
        Order savedOrder = orderRepository.save(order);

        eventPublisher.publishEvent(new OrderDeliveredEvent(
                savedOrder.getId(),
                savedOrder.getBuyerId(),
                savedOrder.getSellerId(),
                savedOrder.getAmount()
        ));

        return savedOrder;
    }

    @Transactional
    public Order disputeOrder(UUID orderId, UUID requesterId, String reason) {
        Order order = getOrderOrThrow(orderId);

        if (!order.getBuyerId().equals(requesterId)) {
            throw new IllegalArgumentException("Hanya pembeli yang dapat mengajukan sengketa.");
        }

        if (!order.getStatus().canTransitionTo(OrderStatus.DISPUTED)) {
            throw new InvalidOrderStatusTransitionException(order.getStatus().name(), OrderStatus.DISPUTED.name());
        }

        order.setStatus(OrderStatus.DISPUTED);
        order.setDisputeReason(reason);
        return orderRepository.save(order);
    }

    @Transactional
    public Order resolveDispute(UUID orderId, boolean refundBuyer) {
        Order order = getOrderOrThrow(orderId);
        
        if (!order.getStatus().canTransitionTo(OrderStatus.CANCELLED) && refundBuyer) {
             throw new InvalidOrderStatusTransitionException(order.getStatus().name(), OrderStatus.CANCELLED.name());
        }

        if (refundBuyer) {
            order.setStatus(OrderStatus.CANCELLED);
            eventPublisher.publishEvent(new OrderRefundedEvent(
                order.getId(), order.getBuyerId(), order.getAmount()
            ));
        } else {
             order.setStatus(OrderStatus.DELIVERED);
             eventPublisher.publishEvent(new OrderDeliveredEvent(
                order.getId(), order.getBuyerId(), order.getSellerId(), order.getAmount()
            ));
        }
        
        return orderRepository.save(order);
    }

    @Transactional
    public void deleteOrder(UUID orderId) {
        Order order = getOrderOrThrow(orderId);
        orderRepository.delete(order);
    }

    private Order getOrderOrThrow(UUID orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Pesanan tidak ditemukan dengan ID: " + orderId));
    }
}