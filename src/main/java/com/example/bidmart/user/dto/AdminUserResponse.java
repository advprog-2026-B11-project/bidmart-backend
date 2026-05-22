package com.example.bidmart.user.dto;

import com.example.bidmart.user.model.User;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class AdminUserResponse {
    private UUID id;
    private String username;
    private String email;
    private String displayName;
    private String imageUrl;
    private String role;
    private boolean active;
    private boolean mfaEnabled;
    private Instant createdAt;

    public static AdminUserResponse from(User user) {
        return AdminUserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .imageUrl(user.getImageUrl())
                .role(user.getRole().getName())
                .active(user.isActive())
                .mfaEnabled(user.isMfaEnabled())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
