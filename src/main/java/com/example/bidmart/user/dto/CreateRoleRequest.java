package com.example.bidmart.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class CreateRoleRequest {
    @NotBlank(message = "Role name is required")
    private String name;
    private String description;
    private List<UUID> permissionIds;
}
