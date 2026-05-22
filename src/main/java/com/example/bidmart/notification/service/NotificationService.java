package com.example.bidmart.notification.service;

import com.example.bidmart.bidding.exception.ResourceNotFoundException;
import com.example.bidmart.notification.model.Notification;
import com.example.bidmart.notification.model.NotificationPreference;
import com.example.bidmart.notification.repository.NotificationPreferenceRepository;
import com.example.bidmart.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public Notification createNotification(UUID userId, String type, String message) {
        NotificationPreference preference = getPreference(userId);

        if (preference.getMutedTypes() != null && preference.getMutedTypes().contains(type)) {
            return null; 
        }

        Notification notification = Notification.builder()
                .userId(userId)
                .type(type)
                .message(message)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .deliveryStatus("PENDING")
                .build();

        if (preference.isInAppEnabled()) {
            notification = notificationRepository.save(notification);
        }

        boolean deliverySuccess = true;

        if (preference.isPushEnabled()) {
            try {
                messagingTemplate.convertAndSendToUser(
                        userId.toString(), "/queue/notifications", notification
                );
            } catch (Exception e) {
                log.error("Gagal mengirim push WebSocket: {}", e.getMessage());
                deliverySuccess = false;
            }
        }

        notification.setDeliveryStatus(deliverySuccess ? "DELIVERED" : "FAILED");
        notificationRepository.save(notification);

        return notification;
    }

    public NotificationPreference getPreference(UUID userId) {
        return preferenceRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultPreference(userId));
    }

    @Transactional
    public NotificationPreference updatePreference(UUID userId, boolean email, boolean push, boolean inApp, List<String> mutedTypes) {
        NotificationPreference pref = getPreference(userId);
        pref.setEmailEnabled(email);
        pref.setPushEnabled(push);
        pref.setInAppEnabled(inApp);
        
        if (mutedTypes != null) {
            pref.setMutedTypes(new HashSet<>(mutedTypes));
        }
        
        return preferenceRepository.save(pref);
    }

    @Transactional
    public NotificationPreference updatePreference(UUID userId, boolean email, boolean push, boolean inApp) {
        return updatePreference(userId, email, push, inApp, null);
    }

    @Transactional
    public NotificationPreference createDefaultPreference(UUID userId) {
        NotificationPreference defaultPref = NotificationPreference.builder()
                .userId(userId)
                .emailEnabled(true)
                .pushEnabled(true)
                .inAppEnabled(true)
                .mutedTypes(new HashSet<>())
                .build();
        return preferenceRepository.save(defaultPref);
    }

    public List<Notification> getUserNotifications(UUID userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public List<Notification> getUnreadNotifications(UUID userId) {
        return notificationRepository.findByUserIdAndIsReadFalse(userId);
    }

    @Transactional
    public Notification markAsRead(UUID notificationId, UUID userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notif tidak ditemukan"));
        if (!notification.getUserId().equals(userId)) {
            throw new org.springframework.security.access.AccessDeniedException("Access Denied");
        }
        notification.setRead(true);
        return notificationRepository.save(notification);
    }

    @Transactional
    public void markAllAsRead(UUID userId) {
        notificationRepository.markAllAsReadByUserId(userId);
    }

    @Transactional
    public void deleteNotification(UUID notificationId, UUID userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notif tidak ditemukan"));
        if (!notification.getUserId().equals(userId)) {
            throw new org.springframework.security.access.AccessDeniedException("Access Denied");
        }
        notificationRepository.delete(notification);
    }
}