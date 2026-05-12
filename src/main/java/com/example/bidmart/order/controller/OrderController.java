package com.example.bidmart.order.controller;

import com.example.bidmart.order.model.Order;
import com.example.bidmart.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<List<Order>> getOrdersByBuyer(@PathVariable UUID buyerId) {
        List<Order> orders = orderService.getOrdersByBuyer(buyerId);
        return ResponseEntity.ok(orders);
    }

    @PatchMapping("/{orderId}/tracking")
    public ResponseEntity<Order> updateTrackingNumber(
            @PathVariable UUID orderId,
            @RequestBody Map<String, String> requestBody) {

        UUID requesterId = UUID.fromString(requestBody.get("requesterId"));
        String trackingNumber = requestBody.get("trackingNumber");

        Order updatedOrder = orderService.updateTrackingNumber(orderId, requesterId, trackingNumber);
        return ResponseEntity.ok(updatedOrder);
    }

    @PatchMapping("/{orderId}/confirm")
    public ResponseEntity<Order> confirmDelivery(
            @PathVariable UUID orderId,
            @RequestBody Map<String, String> requestBody) {

        UUID requesterId = UUID.fromString(requestBody.get("requesterId"));
        Order updatedOrder = orderService.confirmDelivery(orderId, requesterId);
        return ResponseEntity.ok(updatedOrder);
    }

    @PatchMapping("/{orderId}/dispute")
    public ResponseEntity<Order> disputeOrder(
            @PathVariable UUID orderId,
            @RequestBody Map<String, String> requestBody) {

        UUID requesterId = UUID.fromString(requestBody.get("requesterId"));
        String reason = requestBody.get("reason");

        Order updatedOrder = orderService.disputeOrder(orderId, requesterId, reason);
        return ResponseEntity.ok(updatedOrder);
    }

    @PatchMapping("/{orderId}/status")
    public ResponseEntity<Order> updateOrderStatus(
            @PathVariable UUID orderId,
            @RequestBody Map<String, String> requestBody) {

        String newStatus = requestBody.get("status");
        Order updatedOrder = orderService.updateOrderStatus(orderId, newStatus);
        return ResponseEntity.ok(updatedOrder);
    }

    @PostMapping("/test-create")
    public ResponseEntity<Order> createOrderTest(@RequestBody Map<String, String> requestBody) {
        UUID listingId = UUID.fromString(requestBody.get("listingId"));
        UUID buyerId = UUID.fromString(requestBody.get("buyerId"));
        UUID sellerId = UUID.fromString(requestBody.get("sellerId"));
        BigDecimal amount = new BigDecimal(requestBody.get("amount"));

        Order newOrder = orderService.createOrderAutomatically(listingId, buyerId, sellerId, amount);
        return ResponseEntity.ok(newOrder);
    }

    @DeleteMapping("/{orderId}")
    public ResponseEntity<Map<String, String>> deleteOrder(@PathVariable UUID orderId) {
        orderService.deleteOrder(orderId);
        return ResponseEntity.ok(Map.of("message", "Pesanan berhasil dihapus"));
    }
}