package com.example.bidmart.notification.dto;

import lombok.Data;

import java.util.List;

@Data
public class NotificationPreferenceRequest {
    private boolean emailEnabled;
    private boolean pushEnabled;
    private boolean inAppEnabled;
    private List<String> mutedTypes;
}