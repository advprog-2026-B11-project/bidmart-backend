package com.example.bidmart.user.controller;

import com.example.bidmart.user.model.Permission;
import com.example.bidmart.user.model.Role;
import com.example.bidmart.user.repository.PermissionRepository;
import com.example.bidmart.user.repository.RoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoleControllerTest {

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PermissionRepository permissionRepository;

    @InjectMocks
    private RoleController roleController;

    private Role mockRole;
    private Permission mockPermission;

    @BeforeEach
    void setUp() {
        mockPermission = new Permission(UUID.randomUUID(), "TEST_PERMISSION");

        mockRole = new Role();
        mockRole.setId(UUID.randomUUID());
        mockRole.setName("USER");
        // Must use a mutable set
        mockRole.setPermissions(new HashSet<>());
    }

    @Test
    void assignPermission_success() {
        when(roleRepository.findByName("USER")).thenReturn(Optional.of(mockRole));
        when(permissionRepository.findByName("TEST_PERMISSION")).thenReturn(Optional.of(mockPermission));

        ResponseEntity<String> response = roleController.assignPermission("USER", "TEST_PERMISSION");

        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody().contains("successfully added"));
        assertTrue(mockRole.getPermissions().contains(mockPermission));

        verify(roleRepository).save(mockRole);
    }

    @Test
    void assignPermission_roleNotFound() {
        when(roleRepository.findByName("UNKNOWN")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> roleController.assignPermission("UNKNOWN", "TEST_PERMISSION"));
    }

    @Test
    void assignPermission_permissionNotFound() {
        when(roleRepository.findByName("USER")).thenReturn(Optional.of(mockRole));
        when(permissionRepository.findByName("UNKNOWN")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> roleController.assignPermission("USER", "UNKNOWN"));
    }

    @Test
    void createRole_success() {
        when(roleRepository.findByName("NEW_ROLE")).thenReturn(Optional.empty());
        
        Role newRole = new Role();
        newRole.setName("NEW_ROLE");
        when(roleRepository.save(any(Role.class))).thenReturn(newRole);

        ResponseEntity<Role> response = roleController.createRole("NEW_ROLE");

        assertEquals(200, response.getStatusCode().value());
        assertEquals("NEW_ROLE", response.getBody().getName());
    }

    @Test
    void createRole_alreadyExists() {
        when(roleRepository.findByName("USER")).thenReturn(Optional.of(mockRole));

        ResponseEntity<Role> response = roleController.createRole("USER");

        assertEquals(400, response.getStatusCode().value());
    }

    @Test
    void revokePermission_success() {
        mockRole.getPermissions().add(mockPermission);
        
        when(roleRepository.findByName("USER")).thenReturn(Optional.of(mockRole));
        when(permissionRepository.findByName("TEST_PERMISSION")).thenReturn(Optional.of(mockPermission));

        ResponseEntity<String> response = roleController.revokePermission("USER", "TEST_PERMISSION");

        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody().contains("successfully revoked"));
        assertFalse(mockRole.getPermissions().contains(mockPermission));

        verify(roleRepository).save(mockRole);
    }

    @Test
    void revokePermission_doesNotHavePermission() {
        when(roleRepository.findByName("USER")).thenReturn(Optional.of(mockRole));
        when(permissionRepository.findByName("TEST_PERMISSION")).thenReturn(Optional.of(mockPermission));

        ResponseEntity<String> response = roleController.revokePermission("USER", "TEST_PERMISSION");

        assertEquals(400, response.getStatusCode().value());
        assertTrue(response.getBody().contains("does not have that permission"));
    }
}
