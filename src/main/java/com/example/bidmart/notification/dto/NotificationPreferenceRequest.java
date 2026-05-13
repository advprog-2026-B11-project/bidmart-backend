package com.example.bidmart.notification.dto;

import lombok.Data;

@Data
public class NotificationPreferenceRequest {
    private boolean emailEnabled;
    private boolean pushEnabled;
    private boolean inAppEnabled;
}