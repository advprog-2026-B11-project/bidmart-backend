package com.example.bidmart.user.controller;

import com.example.bidmart.user.dto.ChangeUserRoleRequest;
import com.example.bidmart.user.model.Role;
import com.example.bidmart.user.repository.RoleRepository;
import com.example.bidmart.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private AdminUserController adminUserController;

    @Test
    void deactivateUser_shouldReturnOk() {
        UUID userId = UUID.randomUUID();

        ResponseEntity<String> response = adminUserController.deactivateUser(userId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(userService, times(1)).deactivateUser(userId);
    }

    @Test
    void changeUserRole_shouldReturnOk() {
        UUID userId = UUID.randomUUID();
        Role role = new Role();
        role.setName("ADMIN");
        ChangeUserRoleRequest request = new ChangeUserRoleRequest();
        request.setRoleName("ADMIN");

        when(roleRepository.findByName("ADMIN")).thenReturn(Optional.of(role));

        ResponseEntity<String> response = adminUserController.changeUserRole(userId, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(userService, times(1)).changeUserRole(userId, role);
    }
}