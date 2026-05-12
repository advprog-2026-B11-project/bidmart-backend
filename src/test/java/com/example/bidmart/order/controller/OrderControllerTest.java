package com.example.bidmart.order.controller;

import com.example.bidmart.order.model.Order;
import com.example.bidmart.order.service.OrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    @Mock
    private OrderService orderService;

    @InjectMocks
    private OrderController orderController;

    @Test
    void getOrdersByBuyer_returnsOk() {
        UUID buyerId = UUID.randomUUID();
        Order order = new Order();
        when(orderService.getOrdersByBuyer(buyerId)).thenReturn(Arrays.asList(order));

        ResponseEntity<List<Order>> response = orderController.getOrdersByBuyer(buyerId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, Objects.requireNonNull(response.getBody()).size());
    }

    @Test
    void updateOrderStatus_returnsOk() {
        UUID orderId = UUID.randomUUID();
        Map<String, String> request = Map.of("status", "PAID");
        Order order = new Order();
        order.setStatus("PAID");

        when(orderService.updateOrderStatus(orderId, "PAID")).thenReturn(order);

        ResponseEntity<Order> response = orderController.updateOrderStatus(orderId, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("PAID", Objects.requireNonNull(response.getBody()).getStatus());
    }

    @Test
    void updateTrackingNumber_returnsOk() {
        UUID orderId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        Map<String, String> request = Map.of(
                "requesterId", requesterId.toString(),
                "trackingNumber", "RESI-123"
        );
        Order order = new Order();
        order.setTrackingNumber("RESI-123");
        order.setStatus("SHIPPED");

        when(orderService.updateTrackingNumber(orderId, requesterId, "RESI-123")).thenReturn(order);

        ResponseEntity<Order> response = orderController.updateTrackingNumber(orderId, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("RESI-123", Objects.requireNonNull(response.getBody()).getTrackingNumber());
        assertEquals("SHIPPED", response.getBody().getStatus());
    }

    @Test
    void confirmDelivery_returnsOk() {
        UUID orderId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        Map<String, String> request = Map.of("requesterId", requesterId.toString());
        Order order = new Order();
        order.setStatus("DELIVERED");

        when(orderService.confirmDelivery(orderId, requesterId)).thenReturn(order);

        ResponseEntity<Order> response = orderController.confirmDelivery(orderId, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("DELIVERED", Objects.requireNonNull(response.getBody()).getStatus());
    }

    @Test
    void disputeOrder_returnsOk() {
        UUID orderId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        String reason = "Barang tidak sesuai deskripsi";
        Map<String, String> request = Map.of(
                "requesterId", requesterId.toString(),
                "reason", reason
        );
        Order order = new Order();
        order.setStatus("DISPUTED");
        order.setDisputeReason(reason);

        when(orderService.disputeOrder(orderId, requesterId, reason)).thenReturn(order);

        ResponseEntity<Order> response = orderController.disputeOrder(orderId, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("DISPUTED", Objects.requireNonNull(response.getBody()).getStatus());
        assertEquals(reason, response.getBody().getDisputeReason());
    }

    @Test
    void createOrderTest_returnsOk() {
        UUID lId = UUID.randomUUID();
        UUID bId = UUID.randomUUID();
        UUID sId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("150000");

        Map<String, String> request = Map.of(
                "listingId", lId.toString(),
                "buyerId", bId.toString(),
                "sellerId", sId.toString(),
                "amount", amount.toString()
        );

        when(orderService.createOrderAutomatically(lId, bId, sId, amount)).thenReturn(new Order());

        ResponseEntity<Order> response = orderController.createOrderTest(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void deleteOrder_returnsOk() {
        UUID orderId = UUID.randomUUID();
        doNothing().when(orderService).deleteOrder(orderId);

        ResponseEntity<Map<String, String>> response = orderController.deleteOrder(orderId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Pesanan berhasil dihapus", Objects.requireNonNull(response.getBody()).get("message"));
    }
}