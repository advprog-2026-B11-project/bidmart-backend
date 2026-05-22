package com.example.bidmart.notification.dto;

import com.example.bidmart.notification.model.Notification;
import com.example.bidmart.notification.model.NotificationPreference;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class NotificationDtoTest {

    @Test
    void testNotificationPreferenceRequest() {
        NotificationPreferenceRequest request1 = new NotificationPreferenceRequest();
        request1.setEmailEnabled(true);
        request1.setPushEnabled(false);
        request1.setInAppEnabled(true);
        request1.setMutedTypes(List.of("PROMO"));

        assertTrue(request1.isEmailEnabled());
        assertFalse(request1.isPushEnabled());
        assertTrue(request1.isInAppEnabled());
        assertEquals(1, request1.getMutedTypes().size());
        assertTrue(request1.getMutedTypes().contains("PROMO"));

        NotificationPreferenceRequest request2 = new NotificationPreferenceRequest();
        request2.setEmailEnabled(true);
        request2.setPushEnabled(false);
        request2.setInAppEnabled(true);
        request2.setMutedTypes(List.of("PROMO"));

        assertEquals(request1, request1);
        assertNotEquals(request1, null);
        assertNotEquals(request1, new Object());
        assertEquals(request1, request2);
        assertEquals(request1.hashCode(), request2.hashCode());
        assertNotNull(request1.toString());

        request2.setEmailEnabled(false);
        assertNotEquals(request1, request2);
        assertNotEquals(request1.hashCode(), request2.hashCode());

        NotificationPreferenceRequest reqNull = new NotificationPreferenceRequest();
        assertNotEquals(reqNull, request1);
        assertNotEquals(request1, reqNull);
        assertEquals(reqNull, new NotificationPreferenceRequest());
        assertNotNull(reqNull.hashCode());
        assertTrue(request1.canEqual(request2));
        assertFalse(request1.canEqual(new Object()));

        NotificationPreferenceRequest reqDiff = new NotificationPreferenceRequest();
        reqDiff.setEmailEnabled(false);
        assertNotEquals(request1, reqDiff);
        assertNotEquals(reqDiff, request1);
        reqDiff.setEmailEnabled(true);
        reqDiff.setPushEnabled(true);
        assertNotEquals(request1, reqDiff);
        assertNotEquals(reqDiff, request1);
        reqDiff.setPushEnabled(false);
        reqDiff.setInAppEnabled(false);
        assertNotEquals(request1, reqDiff);
        assertNotEquals(reqDiff, request1);
        reqDiff.setInAppEnabled(true);
        reqDiff.setMutedTypes(List.of("OTHER"));
        assertNotEquals(request1, reqDiff);
        assertNotEquals(reqDiff, request1);
    }

    @Test
    void testNotificationModel() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        Notification notif1 = Notification.builder()
                .id(id)
                .userId(userId)
                .type("ALERT")
                .message("Hello")
                .isRead(false)
                .deliveryStatus("PENDING")
                .createdAt(now)
                .build();

        assertEquals(id, notif1.getId());
        assertEquals(userId, notif1.getUserId());
        assertEquals("ALERT", notif1.getType());
        assertEquals("Hello", notif1.getMessage());
        assertFalse(notif1.isRead());
        assertEquals("PENDING", notif1.getDeliveryStatus());
        assertEquals(now, notif1.getCreatedAt());

        Notification notif2 = Notification.builder()
                .id(id)
                .userId(userId)
                .type("ALERT")
                .message("Hello")
                .isRead(false)
                .deliveryStatus("PENDING")
                .createdAt(now)
                .build();

        Notification notif3 = new Notification();
        notif3.setId(id);
        notif3.setUserId(userId);
        notif3.setType("ALERT");
        notif3.setMessage("Hello");
        notif3.setRead(false);
        assertNotNull(notif1.toString());
        assertNotNull(Notification.builder().toString());
    }

    @Test
    void testNotificationPreferenceModel() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        NotificationPreference pref1 = NotificationPreference.builder()
                .id(id)
                .userId(userId)
                .emailEnabled(true)
                .pushEnabled(false)
                .inAppEnabled(true)
                .mutedTypes(new HashSet<>(List.of("SPAM")))
                .build();

        assertEquals(id, pref1.getId());
        assertEquals(userId, pref1.getUserId());
        assertTrue(pref1.isEmailEnabled());
        assertFalse(pref1.isPushEnabled());
        assertTrue(pref1.isInAppEnabled());
        assertTrue(pref1.getMutedTypes().contains("SPAM"));

        NotificationPreference pref2 = NotificationPreference.builder()
                .id(id)
                .userId(userId)
                .emailEnabled(true)
                .pushEnabled(false)
                .inAppEnabled(true)
                .mutedTypes(new HashSet<>(List.of("SPAM")))
                .build();

        NotificationPreference pref3 = new NotificationPreference();
        pref3.setId(id);
        pref3.setUserId(userId);
        pref3.setEmailEnabled(true);
        pref3.setPushEnabled(false);
        assertNotNull(pref1.toString());
        assertNotNull(NotificationPreference.builder().toString());
    }
}
