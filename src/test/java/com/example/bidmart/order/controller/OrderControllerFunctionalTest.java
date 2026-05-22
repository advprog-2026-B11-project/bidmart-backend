package com.example.bidmart.order.controller;

import com.example.bidmart.order.exception.InvalidOrderStatusTransitionException;
import com.example.bidmart.order.exception.OrderNotFoundException;
import com.example.bidmart.order.model.Order;
import com.example.bidmart.order.model.OrderStatus;
import com.example.bidmart.order.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class OrderControllerFunctionalTest {

    private MockMvc mockMvc;

    @Mock
    private OrderService orderService;

    @InjectMocks
    private OrderController orderController;

    private Order order;
    private UUID orderId;
    private UUID buyerId;
    private UUID sellerId;
    private UUID listingId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(orderController).build();

        orderId = UUID.randomUUID();
        buyerId = UUID.randomUUID();
        sellerId = UUID.randomUUID();
        listingId = UUID.randomUUID();

        order = Order.builder()
                .id(orderId)
                .listingId(listingId)
                .buyerId(buyerId)
                .sellerId(sellerId)
                .amount(new BigDecimal("150000.00"))
                .status(OrderStatus.CREATED)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private Principal mockPrincipal(UUID userId) {
        return () -> userId.toString();
    }

    
    @Test
    void testDisputeOrder_MissingParameters_ShouldReturn400() throws Exception {
        mockMvc.perform(patch("/api/orders/{orderId}/dispute", orderId)
                .principal(mockPrincipal(buyerId))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest()); 
    }

    @Test
    void testResolveDispute_ShouldReturnResolvedOrder() throws Exception {
        order.setStatus(OrderStatus.DELIVERED);
        when(orderService.resolveDispute(eq(orderId), eq(true))).thenReturn(order);

        String jsonRequest = "{\"refundBuyer\": true}";

        mockMvc.perform(patch("/api/orders/{orderId}/resolve-dispute", orderId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DELIVERED"));
    }

    @Test
    void testUpdateOrderStatus_ShouldReturnUpdatedOrder() throws Exception {
        order.setStatus(OrderStatus.SHIPPED);
        when(orderService.updateOrderStatus(eq(orderId), eq("SHIPPED"))).thenReturn(order);

        String jsonRequest = "{\"status\": \"SHIPPED\"}";

        mockMvc.perform(patch("/api/orders/{orderId}/status", orderId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SHIPPED"));
    }

    @Test
    void testCreateOrderTest_ShouldReturnNewOrder() throws Exception {
        when(orderService.createOrderAutomatically(eq(listingId), eq(buyerId), eq(sellerId), any(BigDecimal.class)))
                .thenReturn(order);

        String jsonRequest = String.format(
            "{\"listingId\":\"%s\", \"buyerId\":\"%s\", \"sellerId\":\"%s\", \"amount\":150000.00}",
            listingId, buyerId, sellerId
        );

        mockMvc.perform(post("/api/orders/test-create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId.toString()));
    }

    @Test
    void testDeleteOrder_ShouldReturnSuccessMessage() throws Exception {
        doNothing().when(orderService).deleteOrder(orderId);

        mockMvc.perform(delete("/api/orders/{orderId}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Pesanan berhasil dihapus"));
    }
}