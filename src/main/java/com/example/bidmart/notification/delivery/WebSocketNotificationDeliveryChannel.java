package com.example.bidmart.notification.delivery;

import com.example.bidmart.notification.model.Notification;
import com.example.bidmart.notification.model.NotificationPreference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketNotificationDeliveryChannel implements NotificationDeliveryChannel {

    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public boolean supports(NotificationPreference preference) {
        return preference.isPushEnabled();
    }

    @Override
    public void send(Notification notification, NotificationPreference preference) {
        try {
            messagingTemplate.convertAndSendToUser(
                    notification.getUserId().toString(),
                    "/queue/notifications",
                    notification
            );
        } catch (Exception e) {
            log.error("Gagal mengirim push WebSocket: {}", e.getMessage());
            throw new RuntimeException("Gagal mengirim push WebSocket", e);
        }
    }
}
