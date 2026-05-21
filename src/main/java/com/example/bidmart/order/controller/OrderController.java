package com.example.bidmart.order.controller;

import com.example.bidmart.order.dto.*;
import com.example.bidmart.order.model.Order;
import com.example.bidmart.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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
//     @PreAuthorize("hasAuthority(T(com.example.bidmart.common.security.PermissionNames).ORDER_READ)")
    @PreAuthorize("hasAuthority('order:read') and (#buyerId.toString() == authentication.name or hasAuthority('SCOPE_ADMIN'))")
    public ResponseEntity<List<Order>> getOrdersByBuyer(@PathVariable UUID buyerId) {
        List<Order> orders = orderService.getOrdersByBuyer(buyerId);
        return ResponseEntity.ok(orders);
    }

//     @PatchMapping("/{orderId}/status")
//     @PreAuthorize("hasAuthority(T(com.example.bidmart.common.security.PermissionNames).ORDER_UPDATE_STATUS)")
//     public ResponseEntity<Order> updateOrderStatus(
    @PatchMapping("/{orderId}/tracking")
    @PreAuthorize("hasAuthority('order:update')")
    public ResponseEntity<Order> updateTrackingNumber(
            @PathVariable UUID orderId,
            @Valid @RequestBody UpdateTrackingRequest request,
            Authentication authentication) {

        UUID requesterId = UUID.fromString(authentication.getName());
        Order updatedOrder = orderService.updateTrackingNumber(orderId, requesterId, request.getTrackingNumber());
        return ResponseEntity.ok(updatedOrder);
    }

//     @PatchMapping("/{orderId}/tracking")
//     @PreAuthorize("hasAuthority(T(com.example.bidmart.common.security.PermissionNames).ORDER_UPDATE_TRACKING)")
//     public ResponseEntity<Order> updateTrackingNumber(
    @PatchMapping("/{orderId}/confirm")
    @PreAuthorize("hasAuthority('order:update')")
    public ResponseEntity<Order> confirmDelivery(
            @PathVariable UUID orderId,
            Authentication authentication) {

        UUID requesterId = UUID.fromString(authentication.getName());
        Order updatedOrder = orderService.confirmDelivery(orderId, requesterId);
        return ResponseEntity.ok(updatedOrder);
    }

//     @PostMapping("/test-create")
//     @PreAuthorize("hasAuthority(T(com.example.bidmart.common.security.PermissionNames).ORDER_CREATE)")
//     public ResponseEntity<Order> createOrderTest(@RequestBody Map<String, String> requestBody) {
//         UUID listingId = UUID.fromString(requestBody.get("listingId"));
//         UUID buyerId = UUID.fromString(requestBody.get("buyerId"));
    @PatchMapping("/{orderId}/dispute")
    @PreAuthorize("hasAuthority('order:update')")
    public ResponseEntity<Order> disputeOrder(
            @PathVariable UUID orderId,
            @Valid @RequestBody DisputeRequest request,
            Authentication authentication) {

        UUID requesterId = UUID.fromString(authentication.getName());
        Order updatedOrder = orderService.disputeOrder(orderId, requesterId, request.getReason());
        return ResponseEntity.ok(updatedOrder);
    }

    @PatchMapping("/{orderId}/resolve-dispute")
    @PreAuthorize("hasAuthority('SCOPE_ADMIN') or hasAuthority('order:resolve')")
    public ResponseEntity<Order> resolveDispute(
            @PathVariable UUID orderId,
            @Valid @RequestBody ResolveDisputeRequest request) {

        Order updatedOrder = orderService.resolveDispute(orderId, request.getRefundBuyer());
        return ResponseEntity.ok(updatedOrder);
    }

    @PatchMapping("/{orderId}/status")
    @PreAuthorize("hasAuthority('order:update-status') or hasAuthority('SCOPE_ADMIN')")
    public ResponseEntity<Order> updateOrderStatus(
            @PathVariable UUID orderId,
            @Valid @RequestBody UpdateOrderStatusRequest request) {

        Order updatedOrder = orderService.updateOrderStatus(orderId, request.getStatus());
        return ResponseEntity.ok(updatedOrder);
    }

    @PostMapping("/test-create")
    @PreAuthorize("hasAuthority('SCOPE_ADMIN')")
    public ResponseEntity<Order> createOrderTest(@Valid @RequestBody CreateOrderRequest request) {
        Order newOrder = orderService.createOrderAutomatically(
                request.getListingId(),
                request.getBuyerId(),
                request.getSellerId(),
                request.getAmount()
        );
        return ResponseEntity.ok(newOrder);
    }

    @DeleteMapping("/{orderId}")
//     @PreAuthorize("hasAuthority(T(com.example.bidmart.common.security.PermissionNames).ORDER_DELETE)")
    @PreAuthorize("hasAuthority('order:delete') or hasAuthority('SCOPE_ADMIN')")
    public ResponseEntity<Map<String, String>> deleteOrder(@PathVariable UUID orderId) {
        orderService.deleteOrder(orderId);
        return ResponseEntity.ok(Map.of("message", "Pesanan berhasil dihapus"));
    }
}