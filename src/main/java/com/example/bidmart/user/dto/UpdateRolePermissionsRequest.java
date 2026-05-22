package com.example.bidmart.user.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class UpdateRolePermissionsRequest {
    private List<UUID> permissionIds;
}
