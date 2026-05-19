package com.example.bidmart.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MfaEmailVerifyRequest {
    @NotBlank(message = "Email MFA code is required")
    private String code;
}
