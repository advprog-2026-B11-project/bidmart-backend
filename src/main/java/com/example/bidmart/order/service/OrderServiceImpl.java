package com.example.bidmart.order.service;

import com.example.bidmart.common.event.OrderDeliveredEvent;
import com.example.bidmart.common.event.OrderRefundedEvent;
import com.example.bidmart.order.exception.InvalidOrderStatusTransitionException;
import com.example.bidmart.order.exception.OrderNotFoundException;
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
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
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

    @Override
    public List<Order> getOrdersByUser(UUID userId) {
        return orderRepository.findByBuyerIdOrSellerId(userId, userId);
    }

    @Override
    @Transactional
    public Order updateOrderStatus(UUID orderId, String newStatusStr) {
        Order order = getOrderOrThrow(orderId);
        OrderStatus previousStatus = order.getStatus();
        OrderStatus newStatus = OrderStatus.fromString(newStatusStr);
        
        if (!order.getStatus().canTransitionTo(newStatus)) {
            throw new InvalidOrderStatusTransitionException(order.getStatus().name(), newStatus.name());
        }
        
        order.setStatus(newStatus);
        Order savedOrder = orderRepository.save(order);

        if (previousStatus != newStatus) {
            publishWalletEventForStatus(savedOrder, newStatus);
        }

        return savedOrder;
    }

    @Override
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

    @Override
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
                savedOrder.getListingId(),
                savedOrder.getBuyerId(),
                savedOrder.getSellerId(),
                savedOrder.getAmount()
        ));

        return savedOrder;
    }

    @Override
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

    @Override
    @Transactional
    public Order resolveDispute(UUID orderId, boolean refundBuyer) {
        Order order = getOrderOrThrow(orderId);

        if (refundBuyer) {
            if (!order.getStatus().canTransitionTo(OrderStatus.CANCELLED)) {
                throw new InvalidOrderStatusTransitionException(order.getStatus().name(), OrderStatus.CANCELLED.name());
            }
            order.setStatus(OrderStatus.CANCELLED);
            eventPublisher.publishEvent(new OrderRefundedEvent(
                order.getId(), order.getListingId(), order.getBuyerId(), order.getAmount()
            ));
        } else {
            if (!order.getStatus().canTransitionTo(OrderStatus.DELIVERED)) {
                throw new InvalidOrderStatusTransitionException(order.getStatus().name(), OrderStatus.DELIVERED.name());
            }
            order.setStatus(OrderStatus.DELIVERED);
            eventPublisher.publishEvent(new OrderDeliveredEvent(
                order.getId(), order.getListingId(), order.getBuyerId(), order.getSellerId(), order.getAmount()
            ));
        }
        
        return orderRepository.save(order);
    }

    @Override
    @Transactional
    public void deleteOrder(UUID orderId) {
        if (!orderRepository.existsById(orderId)) {
            throw new OrderNotFoundException("Pesanan tidak ditemukan dengan ID: " + orderId);
        }
        orderRepository.deleteById(orderId);
    }

    private Order getOrderOrThrow(UUID orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Pesanan tidak ditemukan dengan ID: " + orderId));
    }

    private void publishWalletEventForStatus(Order order, OrderStatus status) {
        if (status == OrderStatus.DELIVERED) {
            eventPublisher.publishEvent(new OrderDeliveredEvent(
                    order.getId(),
                    order.getListingId(),
                    order.getBuyerId(),
                    order.getSellerId(),
                    order.getAmount()
            ));
        } else if (status == OrderStatus.CANCELLED) {
            eventPublisher.publishEvent(new OrderRefundedEvent(
                    order.getId(),
                    order.getListingId(),
                    order.getBuyerId(),
                    order.getAmount()
            ));
        }
    }
}
