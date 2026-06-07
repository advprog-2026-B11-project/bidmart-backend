package com.example.bidmart.notification.service;

import com.example.bidmart.notification.model.Notification;
import com.example.bidmart.notification.model.NotificationPreference;

import java.util.List;
import java.util.UUID;

public interface NotificationService {
    Notification createNotification(UUID userId, String type, String message);
    NotificationPreference getPreference(UUID userId);
    NotificationPreference updatePreference(UUID userId, boolean email, boolean push, boolean inApp, List<String> mutedTypes);
    NotificationPreference updatePreference(UUID userId, boolean email, boolean push, boolean inApp);
    NotificationPreference createDefaultPreference(UUID userId);
    List<Notification> getUserNotifications(UUID userId);
    List<Notification> getUnreadNotifications(UUID userId);
    Notification markAsRead(UUID notificationId, UUID userId);
    void markAllAsRead(UUID userId);
    void deleteNotification(UUID notificationId, UUID userId);
}