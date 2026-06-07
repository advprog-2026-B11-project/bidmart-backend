package com.example.bidmart.notification.service;

import com.example.bidmart.bidding.exception.ResourceNotFoundException;
import com.example.bidmart.notification.model.Notification;
import com.example.bidmart.notification.model.NotificationPreference;
import com.example.bidmart.notification.repository.NotificationPreferenceRepository;
import com.example.bidmart.notification.repository.NotificationRepository;
import com.example.bidmart.notification.delivery.NotificationDeliveryChannel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final List<NotificationDeliveryChannel> deliveryChannels;

    @Override
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

        for (NotificationDeliveryChannel channel : deliveryChannels) {
            if (channel.supports(preference)) {
                try {
                    channel.send(notification, preference);
                } catch (Exception e) {
                    log.error("Gagal mengirim notifikasi via kanal {}: {}", channel.getClass().getSimpleName(), e.getMessage());
                    deliverySuccess = false;
                }
            }
        }

        notification.setDeliveryStatus(deliverySuccess ? "DELIVERED" : "FAILED");
        notificationRepository.save(notification);

        return notification;
    }

    @Override
    public NotificationPreference getPreference(UUID userId) {
        return preferenceRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultPreference(userId));
    }

    @Override
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

    @Override
    @Transactional
    public NotificationPreference updatePreference(UUID userId, boolean email, boolean push, boolean inApp) {
        return updatePreference(userId, email, push, inApp, null);
    }

    @Override
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

    @Override
    public List<Notification> getUserNotifications(UUID userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    public List<Notification> getUnreadNotifications(UUID userId) {
        return notificationRepository.findByUserIdAndIsReadFalse(userId);
    }

    @Override
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

    @Override
    @Transactional
    public void markAllAsRead(UUID userId) {
        notificationRepository.markAllAsReadByUserId(userId);
    }

    @Override
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
