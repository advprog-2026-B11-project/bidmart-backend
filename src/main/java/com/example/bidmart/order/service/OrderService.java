package com.example.bidmart.order.service;

import com.example.bidmart.bidding.exception.ResourceNotFoundException; // Memakai exception yang sudah ada
import com.example.bidmart.common.event.OrderDeliveredEvent;
import com.example.bidmart.order.model.Order;
import com.example.bidmart.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Order createOrderAutomatically(UUID listingId, UUID buyerId, UUID sellerId, java.math.BigDecimal amount) {
        Order newOrder = Order.builder()
                .listingId(listingId)
                .buyerId(buyerId)
                .sellerId(sellerId)
                .amount(amount)
                .status("CREATED")
                .createdAt(java.time.LocalDateTime.now())
                .build();
        return orderRepository.save(newOrder);
    }

    public List<Order> getOrdersByBuyer(UUID buyerId) {
        return orderRepository.findByBuyerId(buyerId);
    }

    @Transactional
    public Order updateOrderStatus(UUID orderId, String newStatus) {
        Order order = getOrderOrThrow(orderId);
        order.setStatus(newStatus);
        return orderRepository.save(order);
    }

    @Transactional
    public Order updateTrackingNumber(UUID orderId, UUID requesterId, String trackingNumber) {
        Order order = getOrderOrThrow(orderId);

        if (!order.getSellerId().equals(requesterId)) {
            throw new IllegalArgumentException("Hanya penjual yang dapat memperbarui nomor resi.");
        }
        order.setTrackingNumber(trackingNumber);
        order.setStatus("SHIPPED");
        return orderRepository.save(order);
    }

    @Transactional
    public Order confirmDelivery(UUID orderId, UUID requesterId) {
        Order order = getOrderOrThrow(orderId);

        if (!order.getBuyerId().equals(requesterId)) {
            throw new IllegalArgumentException("Hanya pembeli yang dapat mengonfirmasi penerimaan barang.");
        }

        order.setStatus("DELIVERED");
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

        order.setStatus("DISPUTED");
        order.setDisputeReason(reason);
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