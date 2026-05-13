package com.example.bidmart.order.controller;

import com.example.bidmart.order.model.Order;
import com.example.bidmart.order.model.OrderStatus;
import com.example.bidmart.order.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    @Mock
    private OrderService orderService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private OrderController orderController;

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
    void getOrdersByBuyer_returnsOk() {
        when(orderService.getOrdersByBuyer(buyerId)).thenReturn(Arrays.asList(order));
        ResponseEntity<List<Order>> response = orderController.getOrdersByBuyer(buyerId);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void updateTrackingNumber_returnsOk() {
        when(authentication.getName()).thenReturn(sellerId.toString());
        when(orderService.updateTrackingNumber(eq(orderId), eq(sellerId), eq("RESI123"))).thenReturn(order);
        
        Map<String, String> request = new HashMap<>();
        request.put("trackingNumber", "RESI123");

        ResponseEntity<Order> response = orderController.updateTrackingNumber(orderId, request, authentication);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void confirmDelivery_returnsOk() {
        when(authentication.getName()).thenReturn(buyerId.toString());
        when(orderService.confirmDelivery(orderId, buyerId)).thenReturn(order);

        ResponseEntity<Order> response = orderController.confirmDelivery(orderId, authentication);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void disputeOrder_returnsOk() {
        when(authentication.getName()).thenReturn(buyerId.toString());
        when(orderService.disputeOrder(eq(orderId), eq(buyerId), eq("Rusak"))).thenReturn(order);

        Map<String, String> request = new HashMap<>();
        request.put("reason", "Rusak");

        ResponseEntity<Order> response = orderController.disputeOrder(orderId, request, authentication);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void resolveDispute_returnsOk() {
        when(orderService.resolveDispute(orderId, true)).thenReturn(order);

        Map<String, Boolean> request = new HashMap<>();
        request.put("refundBuyer", true);

        ResponseEntity<Order> response = orderController.resolveDispute(orderId, request);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void updateOrderStatus_returnsOk() {
        when(orderService.updateOrderStatus(orderId, "SHIPPED")).thenReturn(order);
        Map<String, String> request = new HashMap<>();
        request.put("status", "SHIPPED");

        ResponseEntity<Order> response = orderController.updateOrderStatus(orderId, request);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void createOrderTest_returnsOk() {
        when(orderService.createOrderAutomatically(eq(listingId), eq(buyerId), eq(sellerId), any(BigDecimal.class)))
                .thenReturn(order);

        Map<String, String> request = new HashMap<>();
        request.put("listingId", listingId.toString());
        request.put("buyerId", buyerId.toString());
        request.put("sellerId", sellerId.toString());
        request.put("amount", "100");

        ResponseEntity<Order> response = orderController.createOrderTest(request);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void deleteOrder_returnsOk() {
        doNothing().when(orderService).deleteOrder(orderId);
        ResponseEntity<Map<String, String>> response = orderController.deleteOrder(orderId);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }
}