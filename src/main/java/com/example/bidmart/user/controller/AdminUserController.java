package com.example.bidmart.user.controller;

import com.example.bidmart.user.dto.ChangeUserRoleRequest;
import com.example.bidmart.user.model.Role;
import com.example.bidmart.user.repository.RoleRepository;
import com.example.bidmart.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@CrossOrigin(origins = "*")
public class AdminUserController {

    private final UserService userService;
    private final RoleRepository roleRepository;

    public AdminUserController(UserService userService, RoleRepository roleRepository) {
        this.userService = userService;
        this.roleRepository = roleRepository;
    }

    @PutMapping("/{userId}/deactivate")
    @PreAuthorize("hasAuthority(T(com.example.bidmart.common.security.PermissionNames).USER_DEACTIVATE)")
    public ResponseEntity<String> deactivateUser(@PathVariable UUID userId) {
        userService.deactivateUser(userId);
        return ResponseEntity.ok("The user account has been successfully deactivated and all sessions have been revoked.");
    }

    @PutMapping("/{userId}/role")
    @PreAuthorize("hasAuthority(T(com.example.bidmart.common.security.PermissionNames).ROLE_MANAGE)")
    public ResponseEntity<String> changeUserRole(
            @PathVariable UUID userId,
            @Valid @RequestBody ChangeUserRoleRequest request) {
        Role role = roleRepository.findByName(request.getRoleName())
                .orElseThrow(() -> new IllegalArgumentException("Role not found."));
        userService.changeUserRole(userId, role);
        return ResponseEntity.ok("User role updated successfully.");
    }
}