package com.example.bidmart.user.service;

import com.example.bidmart.common.event.UserDeactivatedEvent;
import com.example.bidmart.common.event.UserRoleChangedEvent;
import com.example.bidmart.user.dto.UpdateProfileRequest;
import com.example.bidmart.user.dto.UserProfileResponse;
import com.example.bidmart.user.model.Role;
import com.example.bidmart.user.model.User;
import com.example.bidmart.user.repository.SessionRepository;
import com.example.bidmart.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private SessionRepository sessionRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private UserServiceImpl userService;

    private User user;
    private final String username = "testuser";
    private final UUID userId = UUID.randomUUID();

    private Role roleBuyer;
    private Role roleSeller;

    @BeforeEach
    void setUp() {
        roleBuyer = new Role(UUID.randomUUID(), "BUYER", null);
        roleSeller = new Role(UUID.randomUUID(), "SELLER", null);

        user = new User();
        user.setId(userId);
        user.setUsername(username);
        user.setEmail("test@test.com");
        user.setRole(roleBuyer);
        user.setActive(true);
    }

    @Test
    void getCurrentUser_returnsProfile() {
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));

        UserProfileResponse response = userService.getCurrentUser(username);

        assertNotNull(response);
        assertEquals(username, response.getUsername());
    }

    @Test
    void updateProfile_updatesFieldsAndReturnsProfile() {
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setDisplayName("New Name");
        request.setPhoneNumber("123456");

        UserProfileResponse response = userService.updateProfile(username, request);

        assertEquals("New Name", response.getDisplayName());
        assertEquals("123456", response.getPhoneNumber());
        verify(userRepository).save(user);
    }

    @Test
    void deactivateUser_deactivatesAndPublishesEvent() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        userService.deactivateUser(userId);

        assertFalse(user.isActive());
        verify(userRepository).save(user);
        verify(sessionRepository).deleteAllByUserId(userId);
        verify(eventPublisher).publishEvent(any(UserDeactivatedEvent.class));
    }

    @Test
    void deactivateUser_alreadyInactive_doesNothing() {
        user.setActive(false);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        userService.deactivateUser(userId);

        verify(userRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void changeUserRole_changesRoleAndPublishesEvent() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        userService.changeUserRole(userId, roleSeller);

        assertEquals(roleSeller, user.getRole());
        verify(userRepository).save(user);
        verify(eventPublisher).publishEvent(any(UserRoleChangedEvent.class));
    }

    @Test
    void changeUserRole_sameRole_doesNothing() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        userService.changeUserRole(userId, roleBuyer);

        verify(userRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void changeUserRole_nullRole_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> userService.changeUserRole(userId, null));
    }

    @Test
    void deleteProfile_deletesUserAndSessions() {
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));

        userService.deleteProfile(username);

        verify(sessionRepository).deleteAllByUserId(userId);
        verify(userRepository).delete(user);
    }

    @Test
    void getUserIdByUsername_returnsId() {
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));

        UUID result = userService.getUserIdByUsername(username);

        assertEquals(userId, result);
    }
}