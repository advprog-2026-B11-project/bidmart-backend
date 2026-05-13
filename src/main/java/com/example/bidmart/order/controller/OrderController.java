package com.example.bidmart.order.controller;

import com.example.bidmart.order.model.Order;
import com.example.bidmart.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
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
    @PreAuthorize("hasAuthority('order:read') and (#buyerId.toString() == authentication.name or hasAuthority('SCOPE_ADMIN'))")
    public ResponseEntity<List<Order>> getOrdersByBuyer(@PathVariable UUID buyerId) {
        List<Order> orders = orderService.getOrdersByBuyer(buyerId);
        return ResponseEntity.ok(orders);
    }

    @PatchMapping("/{orderId}/tracking")
    @PreAuthorize("hasAuthority('order:update')")
    public ResponseEntity<Order> updateTrackingNumber(
            @PathVariable UUID orderId,
            @RequestBody Map<String, String> requestBody,
            Authentication authentication) {

        UUID requesterId = UUID.fromString(authentication.getName());
        String trackingNumber = requestBody.get("trackingNumber");

        Order updatedOrder = orderService.updateTrackingNumber(orderId, requesterId, trackingNumber);
        return ResponseEntity.ok(updatedOrder);
    }

    @PatchMapping("/{orderId}/confirm")
    @PreAuthorize("hasAuthority('order:update')")
    public ResponseEntity<Order> confirmDelivery(
            @PathVariable UUID orderId,
            Authentication authentication) {

        UUID requesterId = UUID.fromString(authentication.getName());
        Order updatedOrder = orderService.confirmDelivery(orderId, requesterId);
        return ResponseEntity.ok(updatedOrder);
    }

    @PatchMapping("/{orderId}/dispute")
    @PreAuthorize("hasAuthority('order:update')")
    public ResponseEntity<Order> disputeOrder(
            @PathVariable UUID orderId,
            @RequestBody Map<String, String> requestBody,
            Authentication authentication) {

        UUID requesterId = UUID.fromString(authentication.getName());
        String reason = requestBody.get("reason");

        Order updatedOrder = orderService.disputeOrder(orderId, requesterId, reason);
        return ResponseEntity.ok(updatedOrder);
    }

    @PatchMapping("/{orderId}/status")
    @PreAuthorize("hasAuthority('order:update-status') or hasAuthority('SCOPE_ADMIN')")
    public ResponseEntity<Order> updateOrderStatus(
            @PathVariable UUID orderId,
            @RequestBody Map<String, String> requestBody) {

        String newStatus = requestBody.get("status");
        Order updatedOrder = orderService.updateOrderStatus(orderId, newStatus);
        return ResponseEntity.ok(updatedOrder);
    }

    @PostMapping("/test-create")
    @PreAuthorize("hasAuthority('SCOPE_ADMIN')")
    public ResponseEntity<Order> createOrderTest(@RequestBody Map<String, String> requestBody) {
        UUID listingId = UUID.fromString(requestBody.get("listingId"));
        UUID buyerId = UUID.fromString(requestBody.get("buyerId"));
        UUID sellerId = UUID.fromString(requestBody.get("sellerId"));
        BigDecimal amount = new BigDecimal(requestBody.get("amount"));

        Order newOrder = orderService.createOrderAutomatically(listingId, buyerId, sellerId, amount);
        return ResponseEntity.ok(newOrder);
    }

    @DeleteMapping("/{orderId}")
    @PreAuthorize("hasAuthority('order:delete') or hasAuthority('SCOPE_ADMIN')")
    public ResponseEntity<Map<String, String>> deleteOrder(@PathVariable UUID orderId) {
        orderService.deleteOrder(orderId);
        return ResponseEntity.ok(Map.of("message", "Pesanan berhasil dihapus"));
    }
}