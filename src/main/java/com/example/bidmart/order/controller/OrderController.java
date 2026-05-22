package com.example.bidmart.order.controller;

import com.example.bidmart.order.dto.*;
import com.example.bidmart.order.model.Order;
import com.example.bidmart.order.service.OrderService;
import com.example.bidmart.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
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
    private final UserService userService;

    @GetMapping("/buyer/{buyerId}")
//     @PreAuthorize("hasAuthority(T(com.example.bidmart.common.security.PermissionNames).ORDER_READ)")
    @PreAuthorize("hasAuthority('order:read') and (#buyerId.toString() == authentication.name or hasAuthority('SCOPE_ADMIN'))")
    public ResponseEntity<List<Order>> getOrdersByBuyer(@PathVariable UUID buyerId) {
        List<Order> orders = orderService.getOrdersByBuyer(buyerId);
      
    private UUID resolveCurrentUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new AccessDeniedException("User belum terautentikasi.");
        }
        return userService.getUserIdByUsername(authentication.getName());
    }

    private boolean isAdmin(Authentication authentication) {
        if (authentication == null) return false;
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("SCOPE_ADMIN") || a.getAuthority().equals("ROLE_ADMIN"));
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Order>> getOrdersByUser(
            @PathVariable("userId") UUID userId,
            Authentication authentication) {
        
        UUID authenticatedUserId = resolveCurrentUserId(authentication);
        if (!authenticatedUserId.equals(userId) && !isAdmin(authentication)) {
            throw new AccessDeniedException("Anda tidak dapat mengakses order milik user lain.");
        }

        List<Order> orders = orderService.getOrdersByUser(userId);
        return ResponseEntity.ok(orders);
    }

//     @PatchMapping("/{orderId}/status")
//     @PreAuthorize("hasAuthority(T(com.example.bidmart.common.security.PermissionNames).ORDER_UPDATE_STATUS)")
//     public ResponseEntity<Order> updateOrderStatus(
    @PatchMapping("/{orderId}/tracking")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Order> updateTrackingNumber(
            @PathVariable("orderId") UUID orderId,
            @Valid @RequestBody UpdateTrackingRequest request,
            Authentication authentication) {

        UUID requesterId = resolveCurrentUserId(authentication);
        Order updatedOrder = orderService.updateTrackingNumber(orderId, requesterId, request.getTrackingNumber());
        return ResponseEntity.ok(updatedOrder);
    }

//     @PatchMapping("/{orderId}/tracking")
//     @PreAuthorize("hasAuthority(T(com.example.bidmart.common.security.PermissionNames).ORDER_UPDATE_TRACKING)")
//     public ResponseEntity<Order> updateTrackingNumber(
    @PatchMapping("/{orderId}/confirm")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Order> confirmDelivery(
            @PathVariable("orderId") UUID orderId,
            Authentication authentication) {

        UUID requesterId = resolveCurrentUserId(authentication);
        Order updatedOrder = orderService.confirmDelivery(orderId, requesterId);
        return ResponseEntity.ok(updatedOrder);
    }

//     @PostMapping("/test-create")
//     @PreAuthorize("hasAuthority(T(com.example.bidmart.common.security.PermissionNames).ORDER_CREATE)")
//     public ResponseEntity<Order> createOrderTest(@RequestBody Map<String, String> requestBody) {
//         UUID listingId = UUID.fromString(requestBody.get("listingId"));
//         UUID buyerId = UUID.fromString(requestBody.get("buyerId"));
    @PatchMapping("/{orderId}/dispute")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Order> disputeOrder(
            @PathVariable("orderId") UUID orderId,
            @Valid @RequestBody DisputeRequest request,
            Authentication authentication) {

        UUID requesterId = resolveCurrentUserId(authentication);
        Order updatedOrder = orderService.disputeOrder(orderId, requesterId, request.getReason());
        return ResponseEntity.ok(updatedOrder);
    }

    @PatchMapping("/{orderId}/resolve-dispute")
    @PreAuthorize("hasAuthority('SCOPE_ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<Order> resolveDispute(
            @PathVariable("orderId") UUID orderId,
            @Valid @RequestBody ResolveDisputeRequest request) {

        Order updatedOrder = orderService.resolveDispute(orderId, request.getRefundBuyer());
        return ResponseEntity.ok(updatedOrder);
    }

    @PatchMapping("/{orderId}/status")
    @PreAuthorize("hasAuthority('SCOPE_ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<Order> updateOrderStatus(
            @PathVariable("orderId") UUID orderId,
            @Valid @RequestBody UpdateOrderStatusRequest request) {

        Order updatedOrder = orderService.updateOrderStatus(orderId, request.getStatus());
        return ResponseEntity.ok(updatedOrder);
    }

    @PostMapping("/test-create")
    @PreAuthorize("hasAuthority('SCOPE_ADMIN') or hasRole('ADMIN')")
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
    @PreAuthorize("hasAuthority('order:delete') or hasAuthority('SCOPE_ADMIN')")
    public ResponseEntity<Map<String, String>> deleteOrder(@PathVariable("orderId") UUID orderId) {
        orderService.deleteOrder(orderId);
        return ResponseEntity.ok(Map.of("message", "Pesanan berhasil dihapus"));
    }
}