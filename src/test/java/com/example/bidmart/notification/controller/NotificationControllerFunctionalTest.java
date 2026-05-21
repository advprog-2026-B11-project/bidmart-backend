package com.example.bidmart.notification.controller;

import com.example.bidmart.bidding.exception.ResourceNotFoundException;
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
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class NotificationControllerFunctionalTest {

    private MockMvc mockMvc;

    @Mock
    private NotificationService notificationService;

    @Mock
    private UserService userService;

    @InjectMocks
    private NotificationController notificationController;

    private UUID userId;
    private UUID notificationId;
    private Notification notification;
    private NotificationPreference preference;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(notificationController).build();

        userId = UUID.randomUUID();
        notificationId = UUID.randomUUID();

        notification = Notification.builder()
                .id(notificationId)
                .userId(userId)
                .type("SYSTEM_ALERT")
                .message("Ini adalah pesan notifikasi fungsional")
                .isRead(false)
                .deliveryStatus("DELIVERED")
                .createdAt(LocalDateTime.now())
                .build();

        preference = NotificationPreference.builder()
                .userId(userId)
                .emailEnabled(true)
                .pushEnabled(true)
                .inAppEnabled(true)
                .mutedTypes(new HashSet<>())
                .build();
    }

    private Principal mockPrincipal() {
        return new UsernamePasswordAuthenticationToken("testuser", null);
    }

    private void mockCurrentUser() {
        when(userService.getUserIdByUsername("testuser")).thenReturn(userId);
    }

    @Test
    void testGetUserNotifications_ShouldReturnList() throws Exception {
        mockCurrentUser();
        when(notificationService.getUserNotifications(userId)).thenReturn(List.of(notification));

        mockMvc.perform(get("/api/notifications/user/{userId}", userId)
                .principal(mockPrincipal())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(notificationId.toString()))
                .andExpect(jsonPath("$[0].message").value("Ini adalah pesan notifikasi fungsional"));
    }

    @Test
    void testGetUnreadNotifications_ShouldReturnList() throws Exception {
        mockCurrentUser();
        when(notificationService.getUnreadNotifications(userId)).thenReturn(List.of(notification));

        mockMvc.perform(get("/api/notifications/user/{userId}/unread", userId)
                .principal(mockPrincipal())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].read").value(false));
    }

    @Test
    void testMarkAsRead_ShouldReturnUpdatedNotification() throws Exception {
        notification.setRead(true);
        mockCurrentUser();
        when(notificationService.markAsRead(notificationId, userId)).thenReturn(notification);

        mockMvc.perform(patch("/api/notifications/{notificationId}/read", notificationId)
                .principal(mockPrincipal())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.read").value(true));
    }

    @Test
    void testMarkAllAsRead_ShouldReturnSuccessMessage() throws Exception {
        mockCurrentUser();
        doNothing().when(notificationService).markAllAsRead(userId);

        mockMvc.perform(patch("/api/notifications/user/{userId}/read-all", userId)
                .principal(mockPrincipal())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Semua notifikasi berhasil ditandai sudah dibaca"));
    }

    @Test
    void testDeleteNotification_ShouldReturnSuccessMessage() throws Exception {
        mockCurrentUser();
        doNothing().when(notificationService).deleteNotification(notificationId, userId);

        mockMvc.perform(delete("/api/notifications/{notificationId}", notificationId)
                .principal(mockPrincipal())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Notifikasi berhasil dihapus"));
    }

    @Test
    void testGetPreferences_ShouldReturnPreferences() throws Exception {
        mockCurrentUser();
        when(notificationService.getPreference(userId)).thenReturn(preference);

        mockMvc.perform(get("/api/notifications/user/{userId}/preferences", userId)
                .principal(mockPrincipal())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emailEnabled").value(true));
    }

    @Test
    void testUpdatePreferences_ShouldReturnUpdatedPreferences() throws Exception {
        mockCurrentUser();
        preference.setEmailEnabled(false);
        preference.setMutedTypes(new HashSet<>(List.of("PROMO")));

        when(notificationService.updatePreference(eq(userId), eq(false), eq(true), eq(true), any()))
                .thenReturn(preference);

        String jsonRequest = "{\"emailEnabled\": false, \"pushEnabled\": true, \"inAppEnabled\": true, \"mutedTypes\": [\"PROMO\"]}";

        mockMvc.perform(put("/api/notifications/user/{userId}/preferences", userId)
                .principal(mockPrincipal())
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emailEnabled").value(false));
    }

    @Test
    void testCreateTestNotification_ShouldReturnCreatedNotification() throws Exception {
        when(notificationService.createNotification(eq(userId), eq("TEST"), eq("Pesan Uji Coba")))
                .thenReturn(notification);

        String jsonRequest = "{\"userId\": \"" + userId + "\", \"type\": \"TEST\", \"message\": \"Pesan Uji Coba\"}";

        mockMvc.perform(post("/api/notifications/test-create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest))
                .andExpect(status().isOk());
    }

    @Test
    void testMarkAsRead_NotificationNotFound_ShouldReturn404() throws Exception {
        mockCurrentUser();
        when(notificationService.markAsRead(any(UUID.class), any(UUID.class)))
                .thenThrow(new ResourceNotFoundException("Notifikasi tidak ditemukan"));

        mockMvc.perform(patch("/api/notifications/{notificationId}/read", UUID.randomUUID())
                .principal(mockPrincipal())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
}
