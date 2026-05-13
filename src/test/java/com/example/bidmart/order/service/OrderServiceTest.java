package com.example.bidmart.order.service;

import com.example.bidmart.bidding.exception.ResourceNotFoundException;
import com.example.bidmart.common.event.OrderDeliveredEvent;
import com.example.bidmart.common.event.OrderRefundedEvent;
import com.example.bidmart.order.exception.InvalidOrderStatusTransitionException;
import com.example.bidmart.order.model.Order;
import com.example.bidmart.order.model.OrderStatus;
import com.example.bidmart.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private OrderService orderService;

    private UUID orderId, listingId, buyerId, sellerId;
    private Order order;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();
        listingId = UUID.randomUUID();
        buyerId = UUID.randomUUID();
        sellerId = UUID.randomUUID();
        order = new Order(listingId, buyerId, sellerId, BigDecimal.valueOf(100), OrderStatus.CREATED);
        order.setId(orderId);
    }

    @Test
    void createOrderAutomatically_success() {
        when(orderRepository.findByListingId(listingId)).thenReturn(Optional.empty());
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        Order result = orderService.createOrderAutomatically(listingId, buyerId, sellerId, BigDecimal.valueOf(100));
        assertNotNull(result);
        assertEquals(OrderStatus.CREATED, result.getStatus());
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    void createOrderAutomatically_existingOrder_returnsExisting() {
        when(orderRepository.findByListingId(listingId)).thenReturn(Optional.of(order));
        Order result = orderService.createOrderAutomatically(listingId, buyerId, sellerId, BigDecimal.valueOf(100));
        assertEquals(order, result);
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void getOrdersByBuyer_success() {
        when(orderRepository.findByBuyerId(buyerId)).thenReturn(Arrays.asList(order));
        List<Order> result = orderService.getOrdersByBuyer(buyerId);
        assertEquals(1, result.size());
    }

    @Test
    void updateOrderStatus_validTransition_success() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        Order result = orderService.updateOrderStatus(orderId, "SHIPPED");
        assertEquals(OrderStatus.SHIPPED, result.getStatus());
    }

    @Test
    void updateOrderStatus_invalidTransition_throwsException() {
        order.setStatus(OrderStatus.DELIVERED);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThrows(InvalidOrderStatusTransitionException.class, () -> {
            orderService.updateOrderStatus(orderId, "CREATED");
        });
    }

    @Test
    void updateTrackingNumber_success() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        Order result = orderService.updateTrackingNumber(orderId, sellerId, "RESI123");
        assertEquals("RESI123", result.getTrackingNumber());
        assertEquals(OrderStatus.SHIPPED, result.getStatus());
    }

    @Test
    void updateTrackingNumber_wrongUser_throwsException() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        assertThrows(IllegalArgumentException.class, () -> {
            orderService.updateTrackingNumber(orderId, buyerId, "RESI123");
        });
    }

    @Test
    void confirmDelivery_success() {
        order.setStatus(OrderStatus.SHIPPED);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        Order result = orderService.confirmDelivery(orderId, buyerId);
        assertEquals(OrderStatus.DELIVERED, result.getStatus());
        verify(eventPublisher, times(1)).publishEvent(any(OrderDeliveredEvent.class));
    }

    @Test
    void confirmDelivery_invalidState_throwsException() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        assertThrows(InvalidOrderStatusTransitionException.class, () -> {
            orderService.confirmDelivery(orderId, buyerId);
        });
    }

    @Test
    void disputeOrder_success() {
        order.setStatus(OrderStatus.SHIPPED);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        Order result = orderService.disputeOrder(orderId, buyerId, "Barang rusak");
        assertEquals(OrderStatus.DISPUTED, result.getStatus());
        assertEquals("Barang rusak", result.getDisputeReason());
    }

    @Test
    void resolveDispute_refundBuyer_success() {
        order.setStatus(OrderStatus.DISPUTED);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        Order result = orderService.resolveDispute(orderId, true);

        assertEquals(OrderStatus.CANCELLED, result.getStatus());
        verify(eventPublisher, times(1)).publishEvent(any(OrderRefundedEvent.class));
    }

    @Test
    void resolveDispute_paySeller_success() {
        order.setStatus(OrderStatus.DISPUTED);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        Order result = orderService.resolveDispute(orderId, false);

        assertEquals(OrderStatus.DELIVERED, result.getStatus());
        verify(eventPublisher, times(1)).publishEvent(any(OrderDeliveredEvent.class));
    }

    @Test
    void resolveDispute_invalidTransition_throwsException() {
        order.setStatus(OrderStatus.CREATED);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThrows(InvalidOrderStatusTransitionException.class, () -> {
            orderService.resolveDispute(orderId, true);
        });
    }

    @Test
    void deleteOrder_success() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        doNothing().when(orderRepository).delete(order);
        orderService.deleteOrder(orderId);
        verify(orderRepository, times(1)).delete(order);
    }
    
    @Test
    void getOrder_notFound_throwsException() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> orderService.deleteOrder(orderId));
    }
}