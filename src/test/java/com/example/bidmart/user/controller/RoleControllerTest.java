package com.example.bidmart.user.controller;

import com.example.bidmart.user.model.Permission;
import com.example.bidmart.user.model.Role;
import com.example.bidmart.user.repository.PermissionRepository;
import com.example.bidmart.user.repository.RoleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.HashSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleControllerTest {

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PermissionRepository permissionRepository;

    @InjectMocks
    private RoleController roleController;

    @Test
    void assignPermission_shouldAddAndSave() {
        Role role = new Role();
        role.setName("USER");
        role.setPermissions(new HashSet<>());
        Permission permission = new Permission();
        permission.setName("bid:place");

        when(roleRepository.findByName("USER")).thenReturn(Optional.of(role));
        when(permissionRepository.findByName("bid:place")).thenReturn(Optional.of(permission));

        ResponseEntity<String> response = roleController.assignPermission("USER", "bid:place");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(role.getPermissions().contains(permission));
        verify(roleRepository, times(1)).save(role);
    }

    @Test
    void revokePermission_shouldRemoveAndSave() {
        Permission permission = new Permission();
        permission.setName("bid:place");

        Role role = new Role();
        role.setName("USER");
        role.setPermissions(new HashSet<>());
        role.getPermissions().add(permission);

        when(roleRepository.findByName("USER")).thenReturn(Optional.of(role));
        when(permissionRepository.findByName("bid:place")).thenReturn(Optional.of(permission));

        ResponseEntity<String> response = roleController.revokePermission("USER", "bid:place");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(role.getPermissions().isEmpty());
        verify(roleRepository, times(1)).save(role);
    }
}
