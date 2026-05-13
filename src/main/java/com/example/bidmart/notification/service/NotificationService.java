package com.example.bidmart.notification.service;

import com.example.bidmart.bidding.exception.ResourceNotFoundException;
import com.example.bidmart.notification.model.Notification;
import com.example.bidmart.notification.model.NotificationPreference;
import com.example.bidmart.notification.repository.NotificationPreferenceRepository;
import com.example.bidmart.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public Notification createNotification(UUID userId, String type, String message) {
        NotificationPreference preference = preferenceRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultPreference(userId));

        Notification notification = null;

        if (preference.isInAppEnabled()) {
            notification = Notification.builder()
                    .userId(userId)
                    .type(type)
                    .message(message)
                    .isRead(false)
                    .createdAt(LocalDateTime.now())
                    .build();
            notification = notificationRepository.save(notification);
        }

        if (preference.isPushEnabled()) {
            messagingTemplate.convertAndSendToUser(
                    userId.toString(),
                    "/queue/notifications",
                    notification != null ? notification : message
            );
        }

        return notification;
    }

    public List<Notification> getUserNotifications(UUID userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public List<Notification> getUnreadNotifications(UUID userId) {
        return notificationRepository.findByUserIdAndIsReadFalse(userId);
    }

    @Transactional
    public Notification markAsRead(UUID notificationId) {
        Notification notification = getNotificationOrThrow(notificationId);
        notification.setRead(true);
        return notificationRepository.save(notification);
    }

    @Transactional
    public void markAllAsRead(UUID userId) {
        List<Notification> unreadNotifications = notificationRepository.findByUserIdAndIsReadFalse(userId);
        unreadNotifications.forEach(notif -> notif.setRead(true));
        notificationRepository.saveAll(unreadNotifications);
    }

    @Transactional
    public void deleteNotification(UUID notificationId) {
        Notification notification = getNotificationOrThrow(notificationId);
        notificationRepository.delete(notification);
    }

    private Notification getNotificationOrThrow(UUID notificationId) {
        return notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notifikasi tidak ditemukan dengan ID: " + notificationId));
    }
}