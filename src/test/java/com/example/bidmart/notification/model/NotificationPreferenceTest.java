package com.example.bidmart.notification.model;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class NotificationPreferenceTest {

    @Test
    void testNoArgsConstructor() {
        NotificationPreference preference = new NotificationPreference();
        assertNotNull(preference);
    }

    @Test
    void testAllArgsConstructorAndGetters() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        
        NotificationPreference preference = new NotificationPreference(id, userId, true, false, true);
        
        assertEquals(id, preference.getId());
        assertEquals(userId, preference.getUserId());
        assertTrue(preference.isEmailEnabled());
        assertFalse(preference.isPushEnabled());
        assertTrue(preference.isInAppEnabled());
    }

    @Test
    void testSetters() {
        NotificationPreference preference = new NotificationPreference();
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        preference.setId(id);
        preference.setUserId(userId);
        preference.setEmailEnabled(false);
        preference.setPushEnabled(true);
        preference.setInAppEnabled(false);

        assertEquals(id, preference.getId());
        assertEquals(userId, preference.getUserId());
        assertFalse(preference.isEmailEnabled());
        assertTrue(preference.isPushEnabled());
        assertFalse(preference.isInAppEnabled());
    }

    @Test
    void testBuilder() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        NotificationPreference preference = NotificationPreference.builder()
                .id(id)
                .userId(userId)
                .emailEnabled(true)
                .pushEnabled(true)
                .inAppEnabled(false)
                .build();

        assertEquals(id, preference.getId());
        assertEquals(userId, preference.getUserId());
        assertTrue(preference.isEmailEnabled());
        assertTrue(preference.isPushEnabled());
        assertFalse(preference.isInAppEnabled());
    }
}