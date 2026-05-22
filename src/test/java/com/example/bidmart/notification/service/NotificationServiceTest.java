package com.example.bidmart.notification.service;

import com.example.bidmart.bidding.exception.ResourceNotFoundException;
import com.example.bidmart.notification.model.Notification;
import com.example.bidmart.notification.model.NotificationPreference;
import com.example.bidmart.notification.repository.NotificationPreferenceRepository;
import com.example.bidmart.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationPreferenceRepository preferenceRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private NotificationService notificationService;

    private UUID userId;
    private UUID notificationId;
    private Notification notification;
    private NotificationPreference preference;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        notificationId = UUID.randomUUID();
        notification = new Notification(userId, "TEST_TYPE", "Test Message");
        notification.setId(notificationId);

        preference = NotificationPreference.builder()
                .userId(userId)
                .emailEnabled(true)
                .pushEnabled(true)
                .inAppEnabled(true)
                .mutedTypes(new HashSet<>())
                .build();
    }

    @Test
    void createNotification_defaultPreference_success() {
        when(preferenceRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(preferenceRepository.save(any(NotificationPreference.class))).thenReturn(preference);
        when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> i.getArgument(0));

        Notification result = notificationService.createNotification(userId, "TEST_TYPE", "Test Message");

        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        assertEquals("DELIVERED", result.getDeliveryStatus());
        verify(notificationRepository, times(2)).save(any(Notification.class));
        verify(messagingTemplate, times(1)).convertAndSendToUser(eq(userId.toString()), eq("/queue/notifications"), any());
    }

    @Test
    void createNotification_typeIsMuted_returnsNull() {
        preference.setMutedTypes(new HashSet<>(List.of("MUTED_TYPE")));
        when(preferenceRepository.findByUserId(userId)).thenReturn(Optional.of(preference));

        Notification result = notificationService.createNotification(userId, "MUTED_TYPE", "Pesan ini di-mute");

        assertNull(result);
        verify(notificationRepository, never()).save(any(Notification.class));
        verify(messagingTemplate, never()).convertAndSendToUser(anyString(), anyString(), any());
    }

    @Test
    void createNotification_pushThrowsException_deliveryStatusFailed() {
        when(preferenceRepository.findByUserId(userId)).thenReturn(Optional.of(preference));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> i.getArgument(0));

        doThrow(new RuntimeException("WebSocket Error"))
                .when(messagingTemplate).convertAndSendToUser(anyString(), anyString(), any());

        Notification result = notificationService.createNotification(userId, "TEST_TYPE", "Pesan Error WS");

        assertNotNull(result);
        assertEquals("FAILED", result.getDeliveryStatus());
        verify(notificationRepository, times(2)).save(any(Notification.class));
    }

    @Test
    void createNotification_pushDisabled_doesNotSendWebSocket() {
        preference.setPushEnabled(false);
        when(preferenceRepository.findByUserId(userId)).thenReturn(Optional.of(preference));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> i.getArgument(0));

        Notification result = notificationService.createNotification(userId, "TEST_TYPE", "Pesan Tanpa Push");

        assertNotNull(result);
        assertEquals("DELIVERED", result.getDeliveryStatus());
        verify(messagingTemplate, never()).convertAndSendToUser(anyString(), anyString(), any());
    }

    @Test
    void updatePreference_withMutedTypes_success() {
        when(preferenceRepository.findByUserId(userId)).thenReturn(Optional.of(preference));
        when(preferenceRepository.save(any(NotificationPreference.class))).thenReturn(preference);

        List<String> mutedTypes = List.of("PROMO", "SPAM");
        NotificationPreference result = notificationService.updatePreference(userId, false, false, false, mutedTypes);

        assertNotNull(result);
        assertTrue(result.getMutedTypes().contains("PROMO"));
        verify(preferenceRepository, times(1)).save(preference);
    }

    @Test
    void createNotification_onlyPushEnabled_doesNotSaveToDbInitially() {
        preference.setInAppEnabled(false);
        preference.setPushEnabled(true);
        when(preferenceRepository.findByUserId(userId)).thenReturn(Optional.of(preference));

        when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> i.getArgument(0));

        Notification result = notificationService.createNotification(userId, "TEST_TYPE", "Test Message");

        assertNotNull(result);
        verify(notificationRepository, times(1)).save(any(Notification.class));
        verify(messagingTemplate, times(1)).convertAndSendToUser(eq(userId.toString()), eq("/queue/notifications"), any());
    }

    @Test
    void updatePreference_success() {
        when(preferenceRepository.findByUserId(userId)).thenReturn(Optional.of(preference));
        when(preferenceRepository.save(any(NotificationPreference.class))).thenReturn(preference);

        NotificationPreference result = notificationService.updatePreference(userId, false, true, true);

        assertNotNull(result);
        assertFalse(result.isEmailEnabled());
        verify(preferenceRepository, times(1)).save(preference);
    }

    @Test
    void getPreference_success() {
        when(preferenceRepository.findByUserId(userId)).thenReturn(Optional.of(preference));
        NotificationPreference result = notificationService.getPreference(userId);

        assertNotNull(result);
        assertEquals(userId, result.getUserId());
    }

    @Test
    void getUserNotifications_returnsList() {
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(userId))
                .thenReturn(Arrays.asList(notification));
        List<Notification> result = notificationService.getUserNotifications(userId);
        assertEquals(1, result.size());
        verify(notificationRepository, times(1)).findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Test
    void getUnreadNotifications_returnsList() {
        when(notificationRepository.findByUserIdAndIsReadFalse(userId))
                .thenReturn(Arrays.asList(notification));
        List<Notification> result = notificationService.getUnreadNotifications(userId);
        assertEquals(1, result.size());
        assertFalse(result.get(0).isRead());
        verify(notificationRepository, times(1)).findByUserIdAndIsReadFalse(userId);
    }

    @Test
    void markAsRead_success() {
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);
        Notification result = notificationService.markAsRead(notificationId, userId);
        assertTrue(result.isRead());
        verify(notificationRepository, times(1)).save(notification);
    }

    @Test
    void markAsRead_notFound_throwsException() {
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.empty());

        Exception exception = assertThrows(ResourceNotFoundException.class, () -> {
            notificationService.markAsRead(notificationId, userId);
        });

        assertTrue(exception.getMessage().toLowerCase().contains("tidak ditemukan"));
        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    void markAllAsRead_success() {
        Notification notif2 = new Notification(userId, "TYPE_2", "Message 2");
        List<Notification> unreadList = Arrays.asList(notification, notif2);
        when(notificationRepository.findByUserIdAndIsReadFalse(userId)).thenReturn(unreadList);

        notificationService.markAllAsRead(userId);

        assertTrue(notification.isRead());
        assertTrue(notif2.isRead());
        verify(notificationRepository, times(1)).saveAll(unreadList);
    }

    @Test
    void deleteNotification_success() {
        doNothing().when(notificationRepository).delete(notification);
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));

        notificationService.deleteNotification(notificationId, userId);

        verify(notificationRepository, times(1)).delete(notification);
    }

    @Test
    void deleteNotification_notFound_throwsException() {
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> notificationService.deleteNotification(notificationId, userId));
        verify(notificationRepository, never()).delete(any(Notification.class));
    }
}