package com.example.bidmart.notification.controller;

import com.example.bidmart.notification.dto.NotificationPreferenceRequest;
import com.example.bidmart.notification.model.Notification;
import com.example.bidmart.notification.model.NotificationPreference;
import com.example.bidmart.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAuthority('notification:read') and (#userId.toString() == authentication.name or hasAuthority('SCOPE_ADMIN'))")
    public ResponseEntity<List<Notification>> getUserNotifications(@PathVariable UUID userId) {
        return ResponseEntity.ok(notificationService.getUserNotifications(userId));
    }

    @GetMapping("/user/{userId}/unread")
    @PreAuthorize("hasAuthority('notification:read') and (#userId.toString() == authentication.name or hasAuthority('SCOPE_ADMIN'))")
    public ResponseEntity<List<Notification>> getUnreadNotifications(@PathVariable UUID userId) {
        return ResponseEntity.ok(notificationService.getUnreadNotifications(userId));
    }

    @PatchMapping("/{notificationId}/read")
    @PreAuthorize("hasAuthority('notification:update')")
    public ResponseEntity<Notification> markAsRead(@PathVariable UUID notificationId, org.springframework.security.core.Authentication authentication) {
        return ResponseEntity.ok(notificationService.markAsRead(notificationId));
    }

    @PatchMapping("/user/{userId}/read-all")
    @PreAuthorize("hasAuthority('notification:update') and #userId.toString() == authentication.name")
    public ResponseEntity<Map<String, String>> markAllAsRead(@PathVariable UUID userId) {
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(Map.of("message", "Semua notifikasi berhasil ditandai sudah dibaca"));
    }

    @DeleteMapping("/{notificationId}")
    @PreAuthorize("hasAuthority('notification:delete')")
    public ResponseEntity<Map<String, String>> deleteNotification(@PathVariable UUID notificationId) {
        notificationService.deleteNotification(notificationId);
        return ResponseEntity.ok(Map.of("message", "Notifikasi berhasil dihapus"));
    }

    @GetMapping("/user/{userId}/preferences")
    @PreAuthorize("hasAuthority('notification:read') and #userId.toString() == authentication.name")
    public ResponseEntity<NotificationPreference> getPreferences(@PathVariable UUID userId) {
        return ResponseEntity.ok(notificationService.getPreference(userId));
    }

    @PutMapping("/user/{userId}/preferences")
    @PreAuthorize("hasAuthority('notification:update') and #userId.toString() == authentication.name")
    public ResponseEntity<NotificationPreference> updatePreferences(
            @PathVariable UUID userId,
            @RequestBody NotificationPreferenceRequest request) {
        
        NotificationPreference updatedPref = notificationService.updatePreference(
                userId,
                request.isEmailEnabled(),
                request.isPushEnabled(),
                request.isInAppEnabled(),
                request.getMutedTypes()
        );
        return ResponseEntity.ok(updatedPref);
    }

    @PostMapping("/test-create")
    @PreAuthorize("hasAuthority('SCOPE_ADMIN')")
    public ResponseEntity<Notification> createTestNotification(@RequestBody Map<String, String> requestBody) {
        UUID userId = UUID.fromString(requestBody.get("userId"));
        String type = requestBody.get("type");
        String message = requestBody.get("message");

        return ResponseEntity.ok(notificationService.createNotification(userId, type, message));
    }
}