package com.example.bidmart.user.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MfaDisableRequest {
    private String password;
    private String totpCode;
}
