package com.example.bidmart.order.controller;

import com.example.bidmart.order.model.Order;
import com.example.bidmart.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping("/buyer/{buyerId}")
    @PreAuthorize("hasAuthority(T(com.example.bidmart.common.security.PermissionNames).ORDER_READ)")
    public ResponseEntity<List<Order>> getOrdersByBuyer(@PathVariable UUID buyerId) {
        List<Order> orders = orderService.getOrdersByBuyer(buyerId);
        return ResponseEntity.ok(orders);
    }

    @PatchMapping("/{orderId}/status")
    @PreAuthorize("hasAuthority(T(com.example.bidmart.common.security.PermissionNames).ORDER_UPDATE_STATUS)")
    public ResponseEntity<Order> updateOrderStatus(
            @PathVariable UUID orderId,
            @RequestBody Map<String, String> requestBody) {

        String newStatus = requestBody.get("status");
        Order updatedOrder = orderService.updateOrderStatus(orderId, newStatus);
        return ResponseEntity.ok(updatedOrder);
    }

    @PatchMapping("/{orderId}/tracking")
    @PreAuthorize("hasAuthority(T(com.example.bidmart.common.security.PermissionNames).ORDER_UPDATE_TRACKING)")
    public ResponseEntity<Order> updateTrackingNumber(
            @PathVariable UUID orderId,
            @RequestBody Map<String, String> requestBody) {

        String trackingNumber = requestBody.get("trackingNumber");
        Order updatedOrder = orderService.updateTrackingNumber(orderId, trackingNumber);
        return ResponseEntity.ok(updatedOrder);
    }

    @PostMapping("/test-create")
    @PreAuthorize("hasAuthority(T(com.example.bidmart.common.security.PermissionNames).ORDER_CREATE)")
    public ResponseEntity<Order> createOrderTest(@RequestBody Map<String, String> requestBody) {
        UUID listingId = UUID.fromString(requestBody.get("listingId"));
        UUID buyerId = UUID.fromString(requestBody.get("buyerId"));

        Order newOrder = orderService.createOrderAutomatically(listingId, buyerId);
        return ResponseEntity.ok(newOrder);
    }

    @DeleteMapping("/{orderId}")
    @PreAuthorize("hasAuthority(T(com.example.bidmart.common.security.PermissionNames).ORDER_DELETE)")
    public ResponseEntity<Map<String, String>> deleteOrder(@PathVariable UUID orderId) {
        orderService.deleteOrder(orderId);
        return ResponseEntity.ok(Map.of("message", "Pesanan berhasil dihapus"));
    }
}