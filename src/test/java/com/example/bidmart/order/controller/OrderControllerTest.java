package com.example.bidmart.order.controller;

import com.example.bidmart.order.dto.*;
import com.example.bidmart.order.model.Order;
import com.example.bidmart.order.model.OrderStatus;
import com.example.bidmart.order.service.OrderService;
import com.example.bidmart.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    @Mock private OrderService orderService;
    @Mock private UserService userService;
    @Mock private Authentication authentication;
    @InjectMocks private OrderController orderController;

    private UUID orderId, listingId, buyerId, sellerId;
    private Order order;

    @BeforeEach
    void setUp() {
        orderId   = UUID.randomUUID();
        listingId = UUID.randomUUID();
        buyerId   = UUID.randomUUID();
        sellerId  = UUID.randomUUID();
        order = new Order(listingId, buyerId, sellerId, BigDecimal.valueOf(100), OrderStatus.CREATED);
        order.setId(orderId);
    }

    // ── getOrdersByBuyer ──────────────────────────────────────────────────────

    @Test
    void getOrdersByBuyer_sameUser_returnsOk() {
        when(authentication.getName()).thenReturn("buyeruser");
        when(userService.getUserIdByUsername("buyeruser")).thenReturn(buyerId);
        when(orderService.getOrdersByBuyer(buyerId)).thenReturn(List.of(order));

        ResponseEntity<List<Order>> response = orderController.getOrdersByBuyer(buyerId, authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void getOrdersByBuyer_asAdmin_canAccessOtherUser() {
        UUID adminId = UUID.randomUUID();
        when(authentication.getName()).thenReturn("admin");
        when(userService.getUserIdByUsername("admin")).thenReturn(adminId);
        GrantedAuthority adminAuthority = () -> "ROLE_ADMIN";
        doReturn(List.of(adminAuthority)).when(authentication).getAuthorities();
        when(orderService.getOrdersByBuyer(buyerId)).thenReturn(List.of(order));

        ResponseEntity<List<Order>> response = orderController.getOrdersByBuyer(buyerId, authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void getOrdersByBuyer_differentUser_notAdmin_throwsAccessDenied() {
        UUID otherId = UUID.randomUUID();
        when(authentication.getName()).thenReturn("other");
        when(userService.getUserIdByUsername("other")).thenReturn(otherId);
        doReturn(Collections.emptyList()).when(authentication).getAuthorities();

        assertThrows(AccessDeniedException.class,
                () -> orderController.getOrdersByBuyer(buyerId, authentication));
    }

    @Test
    void getOrdersByBuyer_nullAuthentication_throwsAccessDenied() {
        assertThrows(AccessDeniedException.class,
                () -> orderController.getOrdersByBuyer(buyerId, null));
    }

    // ── isAdmin ───────────────────────────────────────────────────────────────

    @Test
    void getOrdersByBuyer_scopeAdminAuthority_isRecognized() {
        UUID adminId = UUID.randomUUID();
        when(authentication.getName()).thenReturn("admin");
        when(userService.getUserIdByUsername("admin")).thenReturn(adminId);
        GrantedAuthority scopeAdmin = () -> "SCOPE_ADMIN";
        doReturn(List.of(scopeAdmin)).when(authentication).getAuthorities();
        when(orderService.getOrdersByBuyer(buyerId)).thenReturn(List.of(order));

        ResponseEntity<List<Order>> response = orderController.getOrdersByBuyer(buyerId, authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    // ── updateTrackingNumber ──────────────────────────────────────────────────

    @Test
    void updateTrackingNumber_returnsOk() {
        when(authentication.getName()).thenReturn(sellerId.toString());
        when(userService.getUserIdByUsername(sellerId.toString())).thenReturn(sellerId);
        when(orderService.updateTrackingNumber(eq(orderId), eq(sellerId), eq("RESI123"))).thenReturn(order);

        UpdateTrackingRequest request = new UpdateTrackingRequest();
        request.setTrackingNumber("RESI123");

        ResponseEntity<Order> response = orderController.updateTrackingNumber(orderId, request, authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    // ── confirmDelivery ───────────────────────────────────────────────────────

    @Test
    void confirmDelivery_returnsOk() {
        when(authentication.getName()).thenReturn(buyerId.toString());
        when(userService.getUserIdByUsername(buyerId.toString())).thenReturn(buyerId);
        when(orderService.confirmDelivery(orderId, buyerId)).thenReturn(order);

        ResponseEntity<Order> response = orderController.confirmDelivery(orderId, authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    // ── disputeOrder ──────────────────────────────────────────────────────────

    @Test
    void disputeOrder_returnsOk() {
        when(authentication.getName()).thenReturn(buyerId.toString());
        when(userService.getUserIdByUsername(buyerId.toString())).thenReturn(buyerId);
        when(orderService.disputeOrder(eq(orderId), eq(buyerId), eq("Rusak"))).thenReturn(order);

        DisputeRequest request = new DisputeRequest();
        request.setReason("Rusak");

        ResponseEntity<Order> response = orderController.disputeOrder(orderId, request, authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    // ── resolveDispute ────────────────────────────────────────────────────────

    @Test
    void resolveDispute_returnsOk() {
        when(orderService.resolveDispute(orderId, true)).thenReturn(order);

        ResolveDisputeRequest request = new ResolveDisputeRequest();
        request.setRefundBuyer(true);

        ResponseEntity<Order> response = orderController.resolveDispute(orderId, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    // ── updateOrderStatus ─────────────────────────────────────────────────────

    @Test
    void updateOrderStatus_returnsOk() {
        when(orderService.updateOrderStatus(orderId, "SHIPPED")).thenReturn(order);

        UpdateOrderStatusRequest request = new UpdateOrderStatusRequest();
        request.setStatus("SHIPPED");

        ResponseEntity<Order> response = orderController.updateOrderStatus(orderId, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    // ── createOrderTest ───────────────────────────────────────────────────────

    @Test
    void createOrderTest_returnsOk() {
        when(orderService.createOrderAutomatically(eq(listingId), eq(buyerId), eq(sellerId), any(BigDecimal.class)))
                .thenReturn(order);

        CreateOrderRequest request = new CreateOrderRequest();
        request.setListingId(listingId);
        request.setBuyerId(buyerId);
        request.setSellerId(sellerId);
        request.setAmount(BigDecimal.valueOf(100));

        ResponseEntity<Order> response = orderController.createOrderTest(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    // ── deleteOrder ───────────────────────────────────────────────────────────

    @Test
    void deleteOrder_returnsOk() {
        doNothing().when(orderService).deleteOrder(orderId);

        ResponseEntity<Map<String, String>> response = orderController.deleteOrder(orderId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }
}
