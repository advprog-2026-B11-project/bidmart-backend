package com.example.bidmart.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserProfileResponse {
    private UUID id;
    private String username;
    private String email;
    private String displayName;
    private String phoneNumber;
    private String role;
    private boolean isEmailVerified;
}
