package com.example.bidmart.user.dto;

import com.example.bidmart.user.model.Permission;
import com.example.bidmart.user.model.Role;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
@Builder
public class AdminRoleResponse {
    private UUID id;
    private String name;
    private String description;
    private boolean system;
    private List<PermissionResponse> permissions;

    @Getter
    @Builder
    public static class PermissionResponse {
        private UUID id;
        private String name;
    }

    public static AdminRoleResponse from(Role role) {
        List<PermissionResponse> perms = role.getPermissions().stream()
                .map(p -> PermissionResponse.builder()
                        .id(p.getId())
                        .name(p.getName())
                        .build())
                .collect(Collectors.toList());

        return AdminRoleResponse.builder()
                .id(role.getId())
                .name(role.getName())
                .description(null)
                .system(false)
                .permissions(perms)
                .build();
    }
}
