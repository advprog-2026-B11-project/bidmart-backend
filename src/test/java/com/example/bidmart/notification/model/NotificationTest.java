package com.example.bidmart.notification.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class NotificationTest {

    @Test
    void testNotificationConstructorsGettersAndSetters() {
        Notification emptyNotification = new Notification();
        assertNotNull(emptyNotification);

        UUID userId = UUID.randomUUID();
        Notification notif = new Notification(userId, "INFO", "Hello World");

        assertEquals(userId, notif.getUserId());
        assertEquals("INFO", notif.getType());
        assertEquals("Hello World", notif.getMessage());
        assertFalse(notif.isRead());
        assertEquals("PENDING", notif.getDeliveryStatus());

        UUID newId = UUID.randomUUID();
        UUID newUserId = UUID.randomUUID();
        LocalDateTime time = LocalDateTime.now();

        notif.setId(newId);
        notif.setUserId(newUserId);
        notif.setType("WARNING");
        notif.setMessage("Updated Message");
        notif.setRead(true);
        notif.setCreatedAt(time);
        notif.setDeliveryStatus("SENT");

        assertEquals(newId, notif.getId());
        assertEquals(newUserId, notif.getUserId());
        assertEquals("WARNING", notif.getType());
        assertEquals("Updated Message", notif.getMessage());
        assertTrue(notif.isRead());
        assertEquals(time, notif.getCreatedAt());
        assertEquals("SENT", notif.getDeliveryStatus());
    }

    @Test
    void testNotificationBuilder() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        LocalDateTime time = LocalDateTime.now();

        Notification notif = Notification.builder()
                .id(id)
                .userId(userId)
                .type("ALERT")
                .message("Builder Message")
                .isRead(true)
                .createdAt(time)
                .deliveryStatus("DELIVERED")
                .build();

        assertEquals(id, notif.getId());
        assertEquals(userId, notif.getUserId());
        assertEquals("ALERT", notif.getType());
        assertEquals("Builder Message", notif.getMessage());
        assertTrue(notif.isRead());
        assertEquals(time, notif.getCreatedAt());
        assertEquals("DELIVERED", notif.getDeliveryStatus());
    }

    @Test
    void testExplicitConstructor() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        LocalDateTime time = LocalDateTime.now();

        Notification notif = new Notification(id, userId, "SYSTEM", "Explicit", false, time);

        assertEquals(id, notif.getId());
        assertEquals(userId, notif.getUserId());
        assertEquals("SYSTEM", notif.getType());
        assertEquals("Explicit", notif.getMessage());
        assertFalse(notif.isRead());
        assertEquals(time, notif.getCreatedAt());
        assertEquals("PENDING", notif.getDeliveryStatus());
    }

    @Test
    void testAllArgsConstructor() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        LocalDateTime time = LocalDateTime.now();

        Notification notif = new Notification(id, userId, "SYSTEM", "All Args", false, time, "SENT");

        assertEquals(id, notif.getId());
        assertEquals(userId, notif.getUserId());
        assertEquals("SYSTEM", notif.getType());
        assertEquals("All Args", notif.getMessage());
        assertFalse(notif.isRead());
        assertEquals(time, notif.getCreatedAt());
        assertEquals("SENT", notif.getDeliveryStatus());
    }

    @Test
    void testPrePersistOnCreate() {
        Notification notif = new Notification();
        assertNull(notif.getCreatedAt());
        notif.onCreate();
        assertNotNull(notif.getCreatedAt());
    }

    @Test
    void testNotificationBuilderDefaultValues() {
        Notification notif = Notification.builder().build();

        assertFalse(notif.isRead());
        assertEquals("PENDING", notif.getDeliveryStatus());
    }

    @Test
    void testDeliveryStatusSetterAndGetter() {
        Notification notif = new Notification();
        notif.setDeliveryStatus("SENT");
        assertEquals("SENT", notif.getDeliveryStatus());
    }
}