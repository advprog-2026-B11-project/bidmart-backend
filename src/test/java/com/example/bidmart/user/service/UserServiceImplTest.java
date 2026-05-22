package com.example.bidmart.user.service;

import com.example.bidmart.common.event.UserDeactivatedEvent;
import com.example.bidmart.common.event.UserRoleChangedEvent;
import com.example.bidmart.user.dto.MfaSetupResponse;
import com.example.bidmart.user.dto.MfaStatusResponse;
import com.example.bidmart.user.dto.UpdateProfileRequest;
import com.example.bidmart.user.dto.UserProfileResponse;
import com.example.bidmart.user.model.MfaMethod;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private SessionRepository sessionRepository;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private MfaService mfaService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private EmailService emailService;

    @InjectMocks
    private UserServiceImpl userService;
    
    private User user;
    private final String username = "alice";

    private Role roleBuyer;
    private Role roleSeller;

    @BeforeEach
    void setUp() {
        roleBuyer = new Role(UUID.randomUUID(), "BUYER", null);
        roleSeller = new Role(UUID.randomUUID(), "SELLER", null);

        user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("alice");
        user.setEmail("alice@mail.com");
        user.setDisplayName("Alice");
        user.setPhoneNumber("08123456789");
        user.setImageUrl("https://img.example.com/alice.png");
        user.setShippingAddress("Jl. Sudirman No. 1, Jakarta");
        user.setRole(roleBuyer);
        user.setEmailVerified(false);
        user.setActive(true);

        userService = new UserServiceImpl(
            userRepository,
            sessionRepository,
            eventPublisher,
            mfaService,
            passwordEncoder,
            emailService,
            300L
        );
    }

    @Test
    void updateProfile_updatesFieldsAndReturnsProfile() {
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setDisplayName("Alice Updated");
        request.setImageUrl("https://img.example.com/alice-new.png");
        request.setPhoneNumber("123456");

        UserProfileResponse response = userService.updateProfile(username, request);

        assertEquals("Alice Updated", response.getDisplayName());
        assertEquals("123456", response.getPhoneNumber());
        verify(userRepository).save(user);
    }

    @Test
    void updateProfile_nullDisplayNameAndPhoneNumber_doesNotUpdateFields() {
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setDisplayName(null);
        request.setPhoneNumber(null);

        UserProfileResponse response = userService.updateProfile(username, request);

        assertEquals("Alice", response.getDisplayName());
        assertEquals("08123456789", response.getPhoneNumber());
        assertEquals("https://img.example.com/alice.png", response.getImageUrl());
        verify(userRepository, times(1)).save(user);
    }

    @Test
    void updateProfile_blankDisplayNameAndPhoneNumber_doesNotUpdateFields() {
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setDisplayName("   ");
        request.setPhoneNumber("   ");

        UserProfileResponse response = userService.updateProfile(username, request);
        assertNotNull(response);
        verify(userRepository).save(user);
    }

    @Test
    void updateProfile_userNotFound_throwsException() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setDisplayName("Alice Updated");
        assertThrows(IllegalArgumentException.class, () -> userService.updateProfile("unknown", request));
    }

    @Test
    void deleteProfile_shouldDeleteSessionsThenUser() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        userService.deleteProfile("alice");

        verify(sessionRepository).deleteAllByUserId(user.getId());
        verify(userRepository).delete(user);
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void getCurrentUser_shouldReturnMappedProfile() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        UserProfileResponse response = userService.getCurrentUser("alice");
        assertNotNull(response);
        assertEquals("alice", response.getUsername());
        assertEquals("alice@mail.com", response.getEmail());
    }

    @Test
    void getMfaStatus_shouldReturnDisabledByDefault() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        MfaStatusResponse response = userService.getMfaStatus("alice");
        assertFalse(response.isEnabled());
        assertEquals("NONE", response.getMethod());
    }

    @Test
    void setupMfa_shouldSetSecretAndReturnQrCode() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(mfaService.generateMfaSecret()).thenReturn("DUMMY_SECRET");
        when(mfaService.getQrCodeImageUri("DUMMY_SECRET", "alice@mail.com")).thenReturn("QR_DATA_URI");

        MfaSetupResponse response = userService.setupMfa("alice");

        assertEquals("DUMMY_SECRET", response.getSecret());
        assertEquals("QR_DATA_URI", response.getQrCodeImageUri());
        assertEquals("TOTP", response.getMethod());
        assertFalse(response.isEnabled());
        
        verify(userRepository, times(1)).save(user);
        assertEquals(MfaMethod.TOTP, user.getMfaMethod());
    }

    @Test
    void enableMfa_shouldThrowExceptionIfSecretNotSet() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        assertThrows(IllegalArgumentException.class, () -> userService.enableMfa("alice", "123456"));
    }

    @Test
    void enableMfa_shouldSucceedWithValidCode() {
        user.setMfaSecret("DUMMY_SECRET");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(mfaService.verifyCode("DUMMY_SECRET", "123456")).thenReturn(true);

        MfaStatusResponse response = userService.enableMfa("alice", "123456");

        assertTrue(response.isEnabled());
        assertEquals("TOTP", response.getMethod());
        assertTrue(user.isMfaEnabled());
        verify(userRepository, times(1)).save(user);
    }

    @Test
    void enableEmailMfa_shouldSetEmailMethod() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        MfaStatusResponse response = userService.enableEmailMfa("alice");

        assertFalse(response.isEnabled());
        assertEquals("EMAIL", response.getMethod());
        assertEquals(MfaMethod.EMAIL, user.getMfaMethod());
        assertNotNull(user.getMfaEmailCode());
        assertNotNull(user.getMfaEmailCodeExpiresAt());
        verify(userRepository, times(1)).save(user);
        verify(emailService, times(1)).sendMfaCodeEmail(eq("alice@mail.com"), anyString());
    }

    @Test
    void verifyEmailMfa_shouldEnableWhenCodeValid() {
        user.setMfaEmailCode("654321");
        user.setMfaEmailCodeExpiresAt(Instant.now().plusSeconds(300));
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        MfaStatusResponse response = userService.verifyEmailMfa("alice", "654321");

        assertTrue(response.isEnabled());
        assertEquals("EMAIL", response.getMethod());
        assertNull(user.getMfaEmailCode());
        assertNull(user.getMfaEmailCodeExpiresAt());
        verify(userRepository, times(1)).save(user);
    }

    @Test
    void disableMfa_withValidPassword_shouldSucceed() {
        user.setMfaEnabled(true);
        user.setMfaEmailCode("123456");
        user.setPassword("encoded_pass");
        
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("raw_pass", "encoded_pass")).thenReturn(true);

        MfaStatusResponse response = userService.disableMfa("alice", "raw_pass", null);

        assertFalse(response.isEnabled());
        assertFalse(user.isMfaEnabled());
        assertNull(user.getMfaEmailCode()); 
        verify(userRepository, times(1)).save(user);
    }

    @Test
    void disableMfa_withoutPasswordOrTotp_shouldThrow() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        assertThrows(IllegalArgumentException.class, () -> userService.disableMfa("alice", "", ""));
    }

    @Test
    void deactivateUser_shouldSetActiveFalseAndRevokeSessions() {
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        userService.deactivateUser(user.getId());

        assertFalse(user.isActive());
        verify(userRepository, times(1)).save(user);
        verify(sessionRepository, times(1)).deleteAllByUserId(user.getId());
        verify(eventPublisher, times(1)).publishEvent(any(UserDeactivatedEvent.class));
    }

    @Test
    void changeUserRole_shouldUpdateRoleAndPublishEvent() {
        Role newRole = new Role(UUID.randomUUID(), "ADMIN", new HashSet<>());
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        userService.changeUserRole(user.getId(), newRole);

        assertEquals(newRole, user.getRole());
        verify(userRepository, times(1)).save(user);
        verify(eventPublisher, times(1)).publishEvent(any(UserRoleChangedEvent.class));
    }

    @Test
    void getUserIdByUsername_shouldReturnId() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        assertEquals(user.getId(), userService.getUserIdByUsername("alice"));
    }
}