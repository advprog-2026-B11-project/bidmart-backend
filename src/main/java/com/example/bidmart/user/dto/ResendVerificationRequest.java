package com.example.bidmart.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResendVerificationRequest {
    @NotBlank(message = "Username or Email cannot be empty")
    private String identifier;
}
