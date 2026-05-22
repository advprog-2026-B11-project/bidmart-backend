package com.example.bidmart.user.controller;

import com.example.bidmart.user.dto.CreateRoleRequest;
import com.example.bidmart.user.dto.UpdateRolePermissionsRequest;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoleControllerTest {

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PermissionRepository permissionRepository;

    @InjectMocks
    private RoleController roleController;

    @Test
    void createRole_shouldReturnOkWhenRoleDoesNotExist() {
        CreateRoleRequest request = new CreateRoleRequest();
        request.setName("NEW_ROLE");

        when(roleRepository.findByName("NEW_ROLE")).thenReturn(Optional.empty());
        Role saved = new Role();
        saved.setName("NEW_ROLE");
        saved.setPermissions(new HashSet<>());
        when(roleRepository.save(any(Role.class))).thenReturn(saved);

        ResponseEntity<?> response = roleController.createRole(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(roleRepository, times(1)).save(any(Role.class));
    }

    @Test
    void createRole_shouldReturnBadRequestWhenRoleExists() {
        CreateRoleRequest request = new CreateRoleRequest();
        request.setName("EXISTING_ROLE");

        when(roleRepository.findByName("EXISTING_ROLE")).thenReturn(Optional.of(new Role()));

        ResponseEntity<?> response = roleController.createRole(request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(roleRepository, never()).save(any(Role.class));
    }

    @Test
    void updateRolePermissions_shouldReplacePermissions() {
        UUID roleId = UUID.randomUUID();
        UUID permId = UUID.randomUUID();

        Role role = new Role();
        role.setName("USER");
        role.setPermissions(new HashSet<>());

        Permission permission = new Permission();
        permission.setName("bid:place");

        UpdateRolePermissionsRequest request = new UpdateRolePermissionsRequest();
        request.setPermissionIds(List.of(permId));

        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));
        when(permissionRepository.findAllById(List.of(permId))).thenReturn(List.of(permission));
        when(roleRepository.save(role)).thenReturn(role);

        ResponseEntity<?> response = roleController.updateRolePermissions(roleId, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(roleRepository, times(1)).save(role);
    }

    @Test
    void updateRolePermissions_shouldThrowWhenRoleNotFound() {
        UUID roleId = UUID.randomUUID();
        UpdateRolePermissionsRequest request = new UpdateRolePermissionsRequest();
        request.setPermissionIds(List.of());

        when(roleRepository.findById(roleId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> roleController.updateRolePermissions(roleId, request));
    }

    @Test
    void deleteRole_shouldReturnNoContent() {
        UUID roleId = UUID.randomUUID();
        Role role = new Role();
        role.setPermissions(new HashSet<>());
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));

        ResponseEntity<Void> response = roleController.deleteRole(roleId);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(roleRepository, times(1)).delete(role);
    }

    @Test
    void deleteRole_shouldThrowWhenRoleNotFound() {
        UUID roleId = UUID.randomUUID();
        when(roleRepository.findById(roleId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> roleController.deleteRole(roleId));
    }

    @Test
    void listRoles_shouldReturnOk() {
        Role role = new Role();
        role.setName("BUYER");
        role.setPermissions(new HashSet<>());
        when(roleRepository.findAll()).thenReturn(List.of(role));

        ResponseEntity<?> response = roleController.listRoles();

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void listPermissions_shouldReturnOk() {
        when(permissionRepository.findAll()).thenReturn(List.of());

        ResponseEntity<?> response = roleController.listPermissions();

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }
}
