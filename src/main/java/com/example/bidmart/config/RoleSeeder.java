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

        // Wallet self-service permissions for end users (buyers & sellers).
        // NOTE: wallet:hold / wallet:release / wallet:settle / wallet:confirm-delivery
        // are intentionally NOT granted here — those endpoints require ROLE_INTERNAL_SERVICE
        // (server-to-server). wallet:create / wallet:list are admin-only.
        Permission walletRead = findOrCreatePermission("wallet:read");
        Permission walletTopUp = findOrCreatePermission("wallet:top-up");
        Permission walletWithdraw = findOrCreatePermission("wallet:withdraw");
        Permission walletTransactionsRead = findOrCreatePermission("wallet:transactions:read");
        Permission walletCreate = findOrCreatePermission("wallet:create");
        Permission walletList = findOrCreatePermission("wallet:list");

        assignPermissions(userRole, List.of(
                notificationRead, notificationUpdate,
                walletRead, walletTopUp, walletWithdraw, walletTransactionsRead));
        assignPermissions(sellerRole, List.of(
                notificationRead, notificationUpdate,
                walletRead, walletTopUp, walletWithdraw, walletTransactionsRead));
        assignPermissions(adminRole, List.of(
                notificationRead, notificationUpdate, notificationDelete,
                walletRead, walletTopUp, walletWithdraw, walletTransactionsRead,
                walletCreate, walletList));
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
