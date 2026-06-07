package com.example.bidmart.notification.delivery;

import com.example.bidmart.notification.model.Notification;
import com.example.bidmart.notification.model.NotificationPreference;
import com.example.bidmart.user.model.User;
import com.example.bidmart.user.repository.UserRepository;
import com.example.bidmart.user.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationDeliveryChannelTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private EmailService emailService;

    @Mock
    private UserRepository userRepository;

    private WebSocketNotificationDeliveryChannel webSocketChannel;
    private EmailNotificationDeliveryChannel emailChannel;

    private UUID userId;
    private Notification notification;
    private NotificationPreference preference;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        notification = Notification.builder()
                .userId(userId)
                .type("TEST_TYPE")
                .message("Test message")
                .build();

        preference = NotificationPreference.builder()
                .userId(userId)
                .pushEnabled(true)
                .emailEnabled(true)
                .build();

        webSocketChannel = new WebSocketNotificationDeliveryChannel(messagingTemplate);
        emailChannel = new EmailNotificationDeliveryChannel(emailService, userRepository);
    }

    @Test
    void webSocketChannel_supports_returnsTrueWhenPushEnabled() {
        assertTrue(webSocketChannel.supports(preference));
        preference.setPushEnabled(false);
        assertFalse(webSocketChannel.supports(preference));
    }

    @Test
    void webSocketChannel_send_success() {
        webSocketChannel.send(notification, preference);
        verify(messagingTemplate, times(1)).convertAndSendToUser(
                eq(userId.toString()), eq("/queue/notifications"), eq(notification)
        );
    }

    @Test
    void webSocketChannel_send_throwsException() {
        doThrow(new RuntimeException("WS Error"))
                .when(messagingTemplate).convertAndSendToUser(anyString(), anyString(), any());
        
        assertThrows(RuntimeException.class, () -> webSocketChannel.send(notification, preference));
    }

    @Test
    void emailChannel_supports_returnsTrueWhenEmailEnabled() {
        assertTrue(emailChannel.supports(preference));
        preference.setEmailEnabled(false);
        assertFalse(emailChannel.supports(preference));
    }

    @Test
    void emailChannel_send_success() {
        User user = new User();
        user.setId(userId);
        user.setEmail("user@example.com");
        user.setDisplayName("Test User");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        emailChannel.send(notification, preference);

        verify(emailService, times(1)).sendNotificationEmail(
                eq("user@example.com"),
                contains("TEST_TYPE"),
                contains("Test message")
        );
    }

    @Test
    void emailChannel_send_userNotFound_doesNotSendEmail() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        emailChannel.send(notification, preference);

        verify(emailService, never()).sendNotificationEmail(any(), any(), any());
    }

    @Test
    void emailChannel_send_throwsException() {
        User user = new User();
        user.setId(userId);
        user.setEmail("user@example.com");
        user.setDisplayName("Test User");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        doThrow(new RuntimeException("Email service failure"))
                .when(emailService).sendNotificationEmail(anyString(), anyString(), anyString());

        assertThrows(RuntimeException.class, () -> emailChannel.send(notification, preference));
    }
}
