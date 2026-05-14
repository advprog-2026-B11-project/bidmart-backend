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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
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

    @Test
    void createRole_shouldReturnOkWhenRoleDoesNotExist() {
        when(roleRepository.findByName("NEW_ROLE")).thenReturn(Optional.empty());
        Role savedRole = new Role();
        savedRole.setName("NEW_ROLE");
        when(roleRepository.save(any(Role.class))).thenReturn(savedRole);

        ResponseEntity<Role> response = roleController.createRole("NEW_ROLE");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("NEW_ROLE", response.getBody().getName());
        verify(roleRepository, times(1)).save(any(Role.class));
    }

    @Test
    void createRole_shouldReturnBadRequestWhenRoleExists() {
        when(roleRepository.findByName("EXISTING_ROLE")).thenReturn(Optional.of(new Role()));
        
        ResponseEntity<Role> response = roleController.createRole("EXISTING_ROLE");
        
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(roleRepository, never()).save(any(Role.class));
    }

    @Test
    void assignPermission_shouldThrowWhenRoleNotFound() {
        when(roleRepository.findByName("UNKNOWN_ROLE")).thenReturn(Optional.empty());
        
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> 
            roleController.assignPermission("UNKNOWN_ROLE", "some:permission"));
        assertEquals("The role was not found", ex.getMessage());
    }

    @Test
    void revokePermission_shouldReturnBadRequestWhenRoleDoesNotHavePermission() {
        Role role = new Role();
        role.setName("USER");
        role.setPermissions(new java.util.HashSet<>());
        Permission permission = new Permission();
        permission.setName("bid:place");

        when(roleRepository.findByName("USER")).thenReturn(Optional.of(role));
        when(permissionRepository.findByName("bid:place")).thenReturn(Optional.of(permission));

        ResponseEntity<String> response = roleController.revokePermission("USER", "bid:place");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("The role does not have that permission", response.getBody());
    }
}
