package com.example.bidmart.user.controller;

import com.example.bidmart.user.dto.AdminRoleResponse;
import com.example.bidmart.user.dto.CreateRoleRequest;
import com.example.bidmart.user.dto.UpdateRolePermissionsRequest;
import com.example.bidmart.user.model.Permission;
import com.example.bidmart.user.model.Role;
import com.example.bidmart.user.repository.PermissionRepository;
import com.example.bidmart.user.repository.RoleRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@PreAuthorize("hasRole('ADMIN') and hasAuthority(T(com.example.bidmart.common.security.PermissionNames).ROLE_MANAGE)")
@CrossOrigin(origins = "*")
public class RoleController {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    public RoleController(RoleRepository roleRepository, PermissionRepository permissionRepository) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
    }

    /** GET /api/admin/roles — list all roles */
    @GetMapping("/api/admin/roles")
    @Transactional(readOnly = true)
    public ResponseEntity<List<AdminRoleResponse>> listRoles() {
        List<AdminRoleResponse> roles = roleRepository.findAll().stream()
                .map(AdminRoleResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(roles);
    }

    /** POST /api/admin/roles — create a new role */
    @PostMapping("/api/admin/roles")
    @Transactional
    public ResponseEntity<AdminRoleResponse> createRole(@Valid @RequestBody CreateRoleRequest request) {
        if (roleRepository.findByName(request.getName()).isPresent()) {
            return ResponseEntity.badRequest().build();
        }
        Role newRole = new Role();
        newRole.setName(request.getName());

        if (request.getPermissionIds() != null && !request.getPermissionIds().isEmpty()) {
            Set<Permission> permissions = new HashSet<>(
                    permissionRepository.findAllById(request.getPermissionIds()));
            newRole.setPermissions(permissions);
        }

        return ResponseEntity.ok(AdminRoleResponse.from(roleRepository.save(newRole)));
    }

    /** PUT /api/admin/roles/{id}/permissions — bulk replace permissions */
    @PutMapping("/api/admin/roles/{id}/permissions")
    @Transactional
    public ResponseEntity<AdminRoleResponse> updateRolePermissions(
            @PathVariable UUID id,
            @RequestBody UpdateRolePermissionsRequest request) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Role not found."));

        Set<Permission> permissions = new HashSet<>();
        if (request.getPermissionIds() != null && !request.getPermissionIds().isEmpty()) {
            permissions.addAll(permissionRepository.findAllById(request.getPermissionIds()));
        }
        role.setPermissions(permissions);
        return ResponseEntity.ok(AdminRoleResponse.from(roleRepository.save(role)));
    }

    /** DELETE /api/admin/roles/{id} — delete a role */
    @DeleteMapping("/api/admin/roles/{id}")
    @Transactional
    public ResponseEntity<Void> deleteRole(@PathVariable UUID id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Role not found."));
        roleRepository.delete(role);
        return ResponseEntity.noContent().build();
    }

    /** GET /api/admin/permissions — list all permissions */
    @GetMapping("/api/admin/permissions")
    @Transactional(readOnly = true)
    public ResponseEntity<List<Permission>> listPermissions() {
        return ResponseEntity.ok(permissionRepository.findAll());
    }
}
