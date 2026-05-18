package com.example.bidmart.notification.dto;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NotificationPreferenceRequestTest {

    @Test
    void testGettersAndSetters() {
        NotificationPreferenceRequest request = new NotificationPreferenceRequest();
        request.setEmailEnabled(true);
        request.setPushEnabled(false);
        request.setInAppEnabled(true);

        assertTrue(request.isEmailEnabled());
        assertFalse(request.isPushEnabled());
        assertTrue(request.isInAppEnabled());
    }
}