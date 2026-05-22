package com.example.bidmart.user.controller;

import com.example.bidmart.user.model.Permission;
import com.example.bidmart.user.model.Role;
import com.example.bidmart.user.repository.PermissionRepository;
import com.example.bidmart.user.repository.RoleRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/roles")
@PreAuthorize("hasRole('ADMIN')")
public class RoleController {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    public RoleController(RoleRepository roleRepository, PermissionRepository permissionRepository) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
    }

    @PostMapping("/{roleName}/permissions/{permissionName}")
    @Transactional
    public ResponseEntity<String> assignPermission(
            @PathVariable("roleName") String roleName,
            @PathVariable("permissionName") String permissionName) {
        
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new IllegalArgumentException("The role was not found"));
        Permission permission = permissionRepository.findByName(permissionName)
                .orElseThrow(() -> new IllegalArgumentException("The Permission was not found"));

        role.getPermissions().add(permission);
        roleRepository.save(role);

        return ResponseEntity.ok("Permission '" + permissionName + "' has been successfully added to role '" + roleName + "'");
    }

    @PostMapping
    public ResponseEntity<Role> createRole(@RequestBody String roleName) {
        if (roleRepository.findByName(roleName).isPresent()) {
            return ResponseEntity.badRequest().build();
        }
        Role newRole = new Role();
        newRole.setName(roleName);
        return ResponseEntity.ok(roleRepository.save(newRole));
    }

    @DeleteMapping("/{roleName}/permissions/{permissionName}")
    @Transactional
    public ResponseEntity<String> revokePermission(
            @PathVariable("roleName") String roleName,
            @PathVariable("permissionName") String permissionName) {
        
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new IllegalArgumentException("The role was not found"));
        Permission permission = permissionRepository.findByName(permissionName)
                .orElseThrow(() -> new IllegalArgumentException("The Permission was not found"));

        if (role.getPermissions().contains(permission)) {
            role.getPermissions().remove(permission);
            roleRepository.save(role);
            return ResponseEntity.ok("Permission '" + permissionName + "' has been successfully revoked from role '" + roleName + "'");
        }
        return ResponseEntity.badRequest().body("The role does not have that permission");
    }
}