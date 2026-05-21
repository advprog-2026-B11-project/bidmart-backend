package com.example.bidmart.config;

import com.example.bidmart.user.model.Permission;
import com.example.bidmart.user.model.Role;
import com.example.bidmart.user.repository.PermissionRepository;
import com.example.bidmart.user.repository.RoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RoleSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    public RoleSeeder(RoleRepository roleRepository, PermissionRepository permissionRepository) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
    }

    @Override
    public void run(String... args) {
        Role userRole = findOrCreateRole("USER");
        Role sellerRole = findOrCreateRole("SELLER");
        Role adminRole = findOrCreateRole("ADMIN");

        Permission notificationRead = findOrCreatePermission("notification:read");
        Permission notificationUpdate = findOrCreatePermission("notification:update");
        Permission notificationDelete = findOrCreatePermission("notification:delete");

        assignPermissions(userRole, List.of(notificationRead, notificationUpdate));
        assignPermissions(sellerRole, List.of(notificationRead, notificationUpdate));
        assignPermissions(adminRole, List.of(notificationRead, notificationUpdate, notificationDelete));
    }

    private Role findOrCreateRole(String name) {
        return roleRepository.findByName(name).orElseGet(() -> {
            Role role = new Role();
            role.setName(name);
            return roleRepository.save(role);
        });
    }

    private Permission findOrCreatePermission(String name) {
        return permissionRepository.findByName(name).orElseGet(() -> {
            Permission permission = new Permission();
            permission.setName(name);
            return permissionRepository.save(permission);
        });
    }

    private void assignPermissions(Role role, List<Permission> permissions) {
        if (role.getPermissions().addAll(permissions)) {
            roleRepository.save(role);
        }
    }
}
