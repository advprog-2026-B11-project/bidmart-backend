package com.example.bidmart.user.controller;

import com.example.bidmart.user.dto.AdminUserResponse;
import com.example.bidmart.user.dto.ChangeUserRoleRequest;
import com.example.bidmart.user.model.Role;
import com.example.bidmart.user.repository.RoleRepository;
import com.example.bidmart.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserService userService;
    private final RoleRepository roleRepository;

    public AdminUserController(UserService userService, RoleRepository roleRepository) {
        this.userService = userService;
        this.roleRepository = roleRepository;
    }

    @GetMapping
    public ResponseEntity<Page<AdminUserResponse>> listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String role) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(userService.listUsers(search, role, pageable));
    }

    @PostMapping("/{userId}/deactivate")
    public ResponseEntity<String> deactivateUser(
            @PathVariable UUID userId,
            @AuthenticationPrincipal UserDetails principal) {
        UUID requesterId = userService.getUserIdByUsername(principal.getUsername());
        if (requesterId.equals(userId)) {
            return ResponseEntity.badRequest().body("Anda tidak dapat menonaktifkan akun Anda sendiri.");
        }
        userService.deactivateUser(userId);
        return ResponseEntity.ok("The user account has been successfully deactivated and all sessions have been revoked.");
    }

    @PostMapping("/{userId}/activate")
    public ResponseEntity<String> activateUser(
            @PathVariable UUID userId,
            @AuthenticationPrincipal UserDetails principal) {
        UUID requesterId = userService.getUserIdByUsername(principal.getUsername());
        if (requesterId.equals(userId)) {
            return ResponseEntity.badRequest().body("Anda tidak dapat mengaktifkan akun Anda sendiri.");
        }
        userService.reactivateUser(userId);
        return ResponseEntity.ok("User account has been successfully reactivated.");
    }

    @PutMapping("/{userId}/role")
    public ResponseEntity<String> changeUserRole(
            @PathVariable UUID userId,
            @Valid @RequestBody ChangeUserRoleRequest request) {
        Role role = roleRepository.findByName(request.getRole())
                .orElseThrow(() -> new IllegalArgumentException("Role not found."));
        userService.changeUserRole(userId, role);
        return ResponseEntity.ok("User role updated successfully.");
    }
}
