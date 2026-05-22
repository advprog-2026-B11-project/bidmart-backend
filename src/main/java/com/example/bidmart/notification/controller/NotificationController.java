package com.example.bidmart.notification.controller;

import com.example.bidmart.notification.dto.NotificationPreferenceRequest;
import com.example.bidmart.notification.model.Notification;
import com.example.bidmart.notification.model.NotificationPreference;
import com.example.bidmart.notification.service.NotificationService;
import com.example.bidmart.user.service.UserService;
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
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final UserService userService;

    private UUID getAuthenticatedUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new AccessDeniedException("Access Denied");
        }
        return userService.getUserIdByUsername(authentication.getName());
    }

    private void ensureCurrentUser(UUID userId, Authentication authentication) {
        UUID authenticatedUserId = getAuthenticatedUserId(authentication);
        if (!authenticatedUserId.equals(userId)) {
            throw new AccessDeniedException("Access Denied");
        }
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Notification>> getUserNotifications(
            @PathVariable("userId") UUID userId,
            Authentication authentication) {
        ensureCurrentUser(userId, authentication);
        return ResponseEntity.ok(notificationService.getUserNotifications(userId));
    }

    @GetMapping("/user/{userId}/unread")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Notification>> getUnreadNotifications(
            @PathVariable("userId") UUID userId,
            Authentication authentication) {
        ensureCurrentUser(userId, authentication);
        return ResponseEntity.ok(notificationService.getUnreadNotifications(userId));
    }

    @PatchMapping("/{notificationId}/read")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Notification> markAsRead(@PathVariable("notificationId") UUID notificationId, Authentication authentication) {
        UUID userId = getAuthenticatedUserId(authentication);
        return ResponseEntity.ok(notificationService.markAsRead(notificationId, userId));
    }

    @PatchMapping("/user/{userId}/read-all")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> markAllAsRead(
            @PathVariable("userId") UUID userId,
            Authentication authentication) {
        ensureCurrentUser(userId, authentication);
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(Map.of("message", "Semua notifikasi berhasil ditandai sudah dibaca"));
    }

    @DeleteMapping("/{notificationId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> deleteNotification(@PathVariable("notificationId") UUID notificationId, Authentication authentication) {
        UUID userId = getAuthenticatedUserId(authentication);
        notificationService.deleteNotification(notificationId, userId);
        return ResponseEntity.ok(Map.of("message", "Notifikasi berhasil dihapus"));
    }

    @GetMapping("/user/{userId}/preferences")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NotificationPreference> getPreferences(
            @PathVariable("userId") UUID userId,
            Authentication authentication) {
        ensureCurrentUser(userId, authentication);
        return ResponseEntity.ok(notificationService.getPreference(userId));
    }

    @PutMapping("/user/{userId}/preferences")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NotificationPreference> updatePreferences(
            @PathVariable("userId") UUID userId,
            @RequestBody NotificationPreferenceRequest request,
            Authentication authentication) {
        ensureCurrentUser(userId, authentication);
        
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
    @PreAuthorize("hasAuthority('SCOPE_ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<Notification> createTestNotification(@RequestBody Map<String, String> requestBody) {
        UUID userId = UUID.fromString(requestBody.get("userId"));
        String type = requestBody.get("type");
        String message = requestBody.get("message");

        return ResponseEntity.ok(notificationService.createNotification(userId, type, message));
    }
}
