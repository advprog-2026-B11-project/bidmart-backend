package com.example.bidmart.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChangeUserRoleRequest {
    @NotBlank(message = "Role name is required")
    private String roleName;
}
