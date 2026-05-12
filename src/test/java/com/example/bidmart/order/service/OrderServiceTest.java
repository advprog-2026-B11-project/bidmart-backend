package com.example.bidmart.order.service;

import com.example.bidmart.bidding.exception.ResourceNotFoundException;
import com.example.bidmart.common.event.OrderDeliveredEvent;
import com.example.bidmart.order.model.Order;
import com.example.bidmart.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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

    private UUID orderId;
    private UUID buyerId;
    private UUID sellerId;
    private UUID listingId;
    private BigDecimal amount;
    private Order order;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();
        buyerId = UUID.randomUUID();
        sellerId = UUID.randomUUID();
        listingId = UUID.randomUUID();
        amount = new BigDecimal("150000.00");

        order = Order.builder()
                .id(orderId)
                .listingId(listingId)
                .buyerId(buyerId)
                .sellerId(sellerId)
                .amount(amount)
                .status("CREATED")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void createOrderAutomatically_success() {
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        Order result = orderService.createOrderAutomatically(listingId, buyerId, sellerId, amount);

        assertNotNull(result);
        assertEquals("CREATED", result.getStatus());
        assertEquals(sellerId, result.getSellerId());
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void getOrdersByBuyer_success() {
        when(orderRepository.findByBuyerId(buyerId)).thenReturn(Arrays.asList(order));
        List<Order> result = orderService.getOrdersByBuyer(buyerId);

        assertEquals(1, result.size());
        assertEquals(orderId, result.get(0).getId());
        verify(orderRepository).findByBuyerId(buyerId);
    }

    @Test
    void updateOrderStatus_success() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        Order result = orderService.updateOrderStatus(orderId, "PAID");
        assertEquals("PAID", result.getStatus());
        verify(orderRepository).save(order);
    }

    @Test
    void updateOrderStatus_notFound_throwsException() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> orderService.updateOrderStatus(orderId, "PAID"));
    }

    @Test
    void updateTrackingNumber_success() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        Order result = orderService.updateTrackingNumber(orderId, sellerId, "RESI123");

        assertEquals("SHIPPED", result.getStatus());
        assertEquals("RESI123", result.getTrackingNumber());
    }

    @Test
    void updateTrackingNumber_unauthorized_throwsException() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        UUID wrongId = UUID.randomUUID(); // Bukan seller

        assertThrows(IllegalArgumentException.class, () -> orderService.updateTrackingNumber(orderId, wrongId, "RESI123"));
    }

    @Test
    void updateTrackingNumber_notFound_throwsException() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> orderService.updateTrackingNumber(orderId, sellerId, "RESI123"));
    }

    @Test
    void confirmDelivery_success() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        Order result = orderService.confirmDelivery(orderId, buyerId);

        assertEquals("DELIVERED", result.getStatus());
        verify(orderRepository).save(order);

        ArgumentCaptor<OrderDeliveredEvent> eventCaptor = ArgumentCaptor.forClass(OrderDeliveredEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        OrderDeliveredEvent capturedEvent = eventCaptor.getValue();
        assertEquals(orderId, capturedEvent.getOrderId());
        assertEquals(buyerId, capturedEvent.getBuyerId());
        assertEquals(sellerId, capturedEvent.getSellerId());
        assertEquals(amount, capturedEvent.getAmount());
    }

    @Test
    void confirmDelivery_unauthorized_throwsException() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        UUID wrongId = UUID.randomUUID();

        assertThrows(IllegalArgumentException.class, () -> orderService.confirmDelivery(orderId, wrongId));
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void disputeOrder_success() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        String reason = "Barang cacat";
        Order result = orderService.disputeOrder(orderId, buyerId, reason);

        assertEquals("DISPUTED", result.getStatus());
        assertEquals(reason, result.getDisputeReason());
        verify(orderRepository).save(order);
    }

    @Test
    void disputeOrder_unauthorized_throwsException() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        UUID wrongId = UUID.randomUUID();

        assertThrows(IllegalArgumentException.class, () -> orderService.disputeOrder(orderId, wrongId, "Cacat"));
    }

    @Test
    void deleteOrder_success() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        orderService.deleteOrder(orderId);
        verify(orderRepository).delete(order);
    }

    @Test
    void deleteOrder_notFound_throwsException() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> orderService.deleteOrder(orderId));
    }
}