package com.example.bidmart.notification.controller;

import com.example.bidmart.notification.dto.NotificationPreferenceRequest;
import com.example.bidmart.notification.model.Notification;
import com.example.bidmart.notification.model.NotificationPreference;
import com.example.bidmart.notification.service.NotificationService;
import com.example.bidmart.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private UserService userService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private NotificationController notificationController;

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
                .build();
    }

    @Test
    void getUserNotifications_returnsOk() {
        when(authentication.getName()).thenReturn("testuser");
        when(userService.getUserIdByUsername("testuser")).thenReturn(userId);
        when(notificationService.getUserNotifications(userId)).thenReturn(Arrays.asList(notification));
        ResponseEntity<List<Notification>> response = notificationController.getUserNotifications(userId, authentication);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void getUnreadNotifications_returnsOk() {
        when(authentication.getName()).thenReturn("testuser");
        when(userService.getUserIdByUsername("testuser")).thenReturn(userId);
        when(notificationService.getUnreadNotifications(userId)).thenReturn(Arrays.asList(notification));
        ResponseEntity<List<Notification>> response = notificationController.getUnreadNotifications(userId, authentication);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertFalse(response.getBody().get(0).isRead());
    }

    @Test
    void markAsRead_returnsOk() {
        notification.setRead(true);
        when(notificationService.markAsRead(notificationId)).thenReturn(notification);
        
        ResponseEntity<Notification> response = notificationController.markAsRead(notificationId, authentication);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isRead());
    }

    @Test
    void createTestNotification_returnsOk() {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("userId", userId.toString());
        requestBody.put("type", "TEST_TYPE");
        requestBody.put("message", "Test Message");

        when(notificationService.createNotification(eq(userId), eq("TEST_TYPE"), eq("Test Message")))
                .thenReturn(notification);

        ResponseEntity<Notification> response = notificationController.createTestNotification(requestBody);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(userId, response.getBody().getUserId());
    }

    @Test
    void markAllAsRead_returnsOk() {
        when(authentication.getName()).thenReturn("testuser");
        when(userService.getUserIdByUsername("testuser")).thenReturn(userId);
        doNothing().when(notificationService).markAllAsRead(userId);
        ResponseEntity<Map<String, String>> response = notificationController.markAllAsRead(userId, authentication);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().containsKey("message"));
    }

    @Test
    void deleteNotification_returnsOk() {
        doNothing().when(notificationService).deleteNotification(notificationId);
        ResponseEntity<Map<String, String>> response = notificationController.deleteNotification(notificationId);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void getPreferences_returnsOk() {
        when(authentication.getName()).thenReturn("testuser");
        when(userService.getUserIdByUsername("testuser")).thenReturn(userId);
        when(notificationService.getPreference(userId)).thenReturn(preference);
        ResponseEntity<NotificationPreference> response = notificationController.getPreferences(userId, authentication);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isEmailEnabled());
    }

    @Test
    void updatePreferences_returnsOk() {
        when(authentication.getName()).thenReturn("testuser");
        when(userService.getUserIdByUsername("testuser")).thenReturn(userId);
        NotificationPreferenceRequest request = new NotificationPreferenceRequest();
        request.setEmailEnabled(false);
        request.setPushEnabled(true);
        request.setInAppEnabled(true);

        when(notificationService.updatePreference(eq(userId), eq(false), eq(true), eq(true), any())).thenReturn(preference);

        ResponseEntity<NotificationPreference> response = notificationController.updatePreferences(userId, request, authentication);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(notificationService, times(1)).updatePreference(eq(userId), eq(false), eq(true), eq(true), any());
    }
}
