package com.example.bidmart.notification.delivery;

import com.example.bidmart.notification.model.Notification;
import com.example.bidmart.notification.model.NotificationPreference;

public interface NotificationDeliveryChannel {
    boolean supports(NotificationPreference preference);
    void send(Notification notification, NotificationPreference preference);
}
