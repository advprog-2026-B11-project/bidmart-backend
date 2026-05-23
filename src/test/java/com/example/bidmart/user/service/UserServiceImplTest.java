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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private SessionRepository sessionRepository;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private MfaService mfaService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private EmailService emailService;

    private UserServiceImpl userService;
    private User user;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(
                userRepository, sessionRepository, eventPublisher,
                mfaService, passwordEncoder, emailService, 300L);

        Role role = new Role(UUID.randomUUID(), "BUYER", new HashSet<>());
        user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setDisplayName("Test User");
        user.setRole(role);
        user.setActive(true);
    }

    // ── getCurrentUser ──────────────────────────────────────────────────────

    @Test
    void getCurrentUser_found_returnsProfile() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        UserProfileResponse resp = userService.getCurrentUser("testuser");

        assertEquals("testuser", resp.getUsername());
        assertEquals("test@example.com", resp.getEmail());
        assertEquals("BUYER", resp.getRole());
    }

    @Test
    void getCurrentUser_notFound_throws() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> userService.getCurrentUser("ghost"));
    }

    // ── updateProfile ───────────────────────────────────────────────────────

    @Test
    void updateProfile_allFields_updatesAll() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setDisplayName("New Name");
        req.setPhoneNumber("0812345678");
        req.setImageUrl("http://img.com/a.png");
        req.setShippingAddress("123 Main St");

        UserProfileResponse resp = userService.updateProfile("testuser", req);

        assertEquals("New Name", resp.getDisplayName());
        assertEquals("0812345678", user.getPhoneNumber());
        assertEquals("123 Main St", user.getShippingAddress());
    }

    @Test
    void updateProfile_nullFields_preservesExistingValues() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        UserProfileResponse resp = userService.updateProfile("testuser", new UpdateProfileRequest());

        assertEquals("Test User", resp.getDisplayName());
    }

    @Test
    void updateProfile_blankFields_preservesExistingValues() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setDisplayName("  ");
        req.setPhoneNumber("  ");

        UserProfileResponse resp = userService.updateProfile("testuser", req);

        assertEquals("Test User", resp.getDisplayName());
        assertNull(user.getPhoneNumber());
    }

    // ── getMfaStatus ────────────────────────────────────────────────────────

    @Test
    void getMfaStatus_enabledTotp_returnsEnabledAndTotp() {
        user.setMfaEnabled(true);
        user.setMfaMethod(MfaMethod.TOTP);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        MfaStatusResponse status = userService.getMfaStatus("testuser");

        assertTrue(status.isEnabled());
        assertEquals("TOTP", status.getMethod());
    }

    @Test
    void getMfaStatus_disabledNullMethod_returnsNone() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        MfaStatusResponse status = userService.getMfaStatus("testuser");

        assertFalse(status.isEnabled());
        assertEquals("NONE", status.getMethod());
    }

    @Test
    void getMfaStatus_enabledEmail_returnsEmail() {
        user.setMfaEnabled(true);
        user.setMfaMethod(MfaMethod.EMAIL);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        MfaStatusResponse status = userService.getMfaStatus("testuser");

        assertTrue(status.isEnabled());
        assertEquals("EMAIL", status.getMethod());
    }

    // ── setupMfa ────────────────────────────────────────────────────────────

    @Test
    void setupMfa_success_returnsSetupData() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(mfaService.generateMfaSecret()).thenReturn("JBSWY3DPEHPK3PXP");
        when(mfaService.getQrCodeImageUri("JBSWY3DPEHPK3PXP", "test@example.com"))
                .thenReturn("data:image/png;base64,abc");

        MfaSetupResponse resp = userService.setupMfa("testuser");

        assertEquals("JBSWY3DPEHPK3PXP", resp.getSecret());
        assertEquals("data:image/png;base64,abc", resp.getQrCodeImageUri());
        assertFalse(resp.isEnabled());
        assertEquals("TOTP", resp.getMethod());
        verify(userRepository).save(user);
    }

    // ── enableMfa ───────────────────────────────────────────────────────────

    @Test
    void enableMfa_validCode_enablesMfa() {
        user.setMfaSecret("SECRET");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(mfaService.verifyCode("SECRET", "123456")).thenReturn(true);

        MfaStatusResponse resp = userService.enableMfa("testuser", "123456");

        assertTrue(resp.isEnabled());
        assertEquals("TOTP", resp.getMethod());
        verify(userRepository).save(user);
    }

    @Test
    void enableMfa_alreadyEnabled_stillSavesMethod() {
        user.setMfaSecret("SECRET");
        user.setMfaEnabled(true);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(mfaService.verifyCode("SECRET", "123456")).thenReturn(true);

        MfaStatusResponse resp = userService.enableMfa("testuser", "123456");

        assertTrue(resp.isEnabled());
        verify(userRepository).save(user);
    }

    @Test
    void enableMfa_nullSecret_throws() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        assertThrows(IllegalArgumentException.class,
                () -> userService.enableMfa("testuser", "123456"));
    }

    @Test
    void enableMfa_blankSecret_throws() {
        user.setMfaSecret("   ");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        assertThrows(IllegalArgumentException.class,
                () -> userService.enableMfa("testuser", "123456"));
    }

    @Test
    void enableMfa_invalidCode_throws() {
        user.setMfaSecret("SECRET");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(mfaService.verifyCode("SECRET", "000000")).thenReturn(false);

        assertThrows(IllegalArgumentException.class,
                () -> userService.enableMfa("testuser", "000000"));
    }

    // ── enableEmailMfa ──────────────────────────────────────────────────────

    @Test
    void enableEmailMfa_success_setsEmailMethodAndSendsCode() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        MfaStatusResponse resp = userService.enableEmailMfa("testuser");

        assertEquals("EMAIL", resp.getMethod());
        verify(emailService).sendMfaCodeEmail(eq("test@example.com"), anyString());
        verify(userRepository).save(user);
    }

    @Test
    void enableEmailMfa_alreadyEnabled_throws() {
        user.setMfaEnabled(true);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        assertThrows(IllegalArgumentException.class,
                () -> userService.enableEmailMfa("testuser"));
    }

    // ── verifyEmailMfa ──────────────────────────────────────────────────────

    @Test
    void verifyEmailMfa_validCode_enablesMfa() {
        user.setMfaEmailCode("123456");
        user.setMfaEmailCodeExpiresAt(Instant.now().plusSeconds(300));
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        MfaStatusResponse resp = userService.verifyEmailMfa("testuser", "123456");

        assertTrue(resp.isEnabled());
        assertEquals("EMAIL", resp.getMethod());
        verify(userRepository).save(user);
    }

    @Test
    void verifyEmailMfa_expiredCode_clearsAndThrows() {
        user.setMfaEmailCode("123456");
        user.setMfaEmailCodeExpiresAt(Instant.now().minusSeconds(1));
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        assertThrows(IllegalArgumentException.class,
                () -> userService.verifyEmailMfa("testuser", "123456"));
        verify(userRepository).save(user);
    }

    @Test
    void verifyEmailMfa_nullCode_throws() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        assertThrows(IllegalArgumentException.class,
                () -> userService.verifyEmailMfa("testuser", "123456"));
    }

    @Test
    void verifyEmailMfa_wrongCode_throws() {
        user.setMfaEmailCode("123456");
        user.setMfaEmailCodeExpiresAt(Instant.now().plusSeconds(300));
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        assertThrows(IllegalArgumentException.class,
                () -> userService.verifyEmailMfa("testuser", "999999"));
    }

    // ── disableMfa ──────────────────────────────────────────────────────────

    @Test
    void disableMfa_withValidPassword_disables() {
        user.setMfaEnabled(true);
        user.setPassword("hashed");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("correct", "hashed")).thenReturn(true);

        MfaStatusResponse resp = userService.disableMfa("testuser", "correct", null);

        assertFalse(resp.isEnabled());
        verify(userRepository).save(user);
    }

    @Test
    void disableMfa_withValidTotp_disables() {
        user.setMfaEnabled(true);
        user.setMfaSecret("SECRET");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(mfaService.verifyCode("SECRET", "123456")).thenReturn(true);

        MfaStatusResponse resp = userService.disableMfa("testuser", null, "123456");

        assertFalse(resp.isEnabled());
        verify(userRepository).save(user);
    }

    @Test
    void disableMfa_noCredentials_throws() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        assertThrows(IllegalArgumentException.class,
                () -> userService.disableMfa("testuser", null, null));
    }

    @Test
    void disableMfa_blankCredentials_throws() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        assertThrows(IllegalArgumentException.class,
                () -> userService.disableMfa("testuser", "  ", "  "));
    }

    @Test
    void disableMfa_totpCodeButNoSecret_throws() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        assertThrows(IllegalArgumentException.class,
                () -> userService.disableMfa("testuser", null, "123456"));
    }

    @Test
    void disableMfa_invalidTotpCode_throws() {
        user.setMfaSecret("SECRET");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(mfaService.verifyCode("SECRET", "000000")).thenReturn(false);

        assertThrows(IllegalArgumentException.class,
                () -> userService.disableMfa("testuser", null, "000000"));
    }

    @Test
    void disableMfa_invalidPassword_throws() {
        user.setPassword("hashed");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        assertThrows(IllegalArgumentException.class,
                () -> userService.disableMfa("testuser", "wrong", null));
    }

    @Test
    void disableMfa_alreadyDisabled_returnsWithoutSaving() {
        user.setPassword("hashed");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("correct", "hashed")).thenReturn(true);

        MfaStatusResponse resp = userService.disableMfa("testuser", "correct", null);

        assertFalse(resp.isEnabled());
        verify(userRepository, never()).save(any());
    }

    // ── listUsers ───────────────────────────────────────────────────────────

    @Test
    void listUsers_withFilters_trimsThenQueries() {
        Page<User> page = new PageImpl<>(List.of());
        when(userRepository.findAllWithFilters("query", "BUYER", Pageable.unpaged()))
                .thenReturn(page);

        userService.listUsers("  query  ", "  BUYER  ", Pageable.unpaged());

        verify(userRepository).findAllWithFilters("query", "BUYER", Pageable.unpaged());
    }

    @Test
    void listUsers_nullFilters_usesEmptyStrings() {
        Page<User> page = new PageImpl<>(List.of());
        when(userRepository.findAllWithFilters("", "", Pageable.unpaged()))
                .thenReturn(page);

        userService.listUsers(null, null, Pageable.unpaged());

        verify(userRepository).findAllWithFilters("", "", Pageable.unpaged());
    }

    @Test
    void listUsers_blankFilters_usesEmptyStrings() {
        Page<User> page = new PageImpl<>(List.of());
        when(userRepository.findAllWithFilters("", "", Pageable.unpaged()))
                .thenReturn(page);

        userService.listUsers("  ", "  ", Pageable.unpaged());

        verify(userRepository).findAllWithFilters("", "", Pageable.unpaged());
    }

    // ── deactivateUser ──────────────────────────────────────────────────────

    @Test
    void deactivateUser_active_deactivatesAndPublishesEvent() {
        UUID userId = user.getId();
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
        UUID userId = user.getId();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        userService.deactivateUser(userId);

        verify(userRepository, never()).save(any());
        verify(sessionRepository, never()).deleteAllByUserId(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void deactivateUser_notFound_throws() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> userService.deactivateUser(userId));
    }

    // ── reactivateUser ──────────────────────────────────────────────────────

    @Test
    void reactivateUser_inactive_reactivates() {
        user.setActive(false);
        UUID userId = user.getId();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        userService.reactivateUser(userId);

        assertTrue(user.isActive());
        verify(userRepository).save(user);
    }

    @Test
    void reactivateUser_alreadyActive_doesNothing() {
        UUID userId = user.getId();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        userService.reactivateUser(userId);

        verify(userRepository, never()).save(any());
    }

    @Test
    void reactivateUser_notFound_throws() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> userService.reactivateUser(userId));
    }

    // ── changeUserRole ──────────────────────────────────────────────────────

    @Test
    void changeUserRole_differentRole_changesAndPublishesEvent() {
        UUID userId = user.getId();
        Role newRole = new Role(UUID.randomUUID(), "SELLER", new HashSet<>());
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        userService.changeUserRole(userId, newRole);

        assertEquals(newRole, user.getRole());
        verify(userRepository).save(user);
        verify(eventPublisher).publishEvent(any(UserRoleChangedEvent.class));
    }

    @Test
    void changeUserRole_sameRole_doesNothing() {
        UUID userId = user.getId();
        Role sameRole = user.getRole();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        userService.changeUserRole(userId, sameRole);

        verify(userRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void changeUserRole_nullRole_throwsBeforeQuery() {
        UUID userId = UUID.randomUUID();

        assertThrows(IllegalArgumentException.class,
                () -> userService.changeUserRole(userId, null));
        verify(userRepository, never()).findById(any());
    }

    @Test
    void changeUserRole_notFound_throws() {
        UUID userId = UUID.randomUUID();
        Role newRole = new Role(UUID.randomUUID(), "SELLER", new HashSet<>());
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> userService.changeUserRole(userId, newRole));
    }

    // ── getUserIdByUsername ─────────────────────────────────────────────────

    @Test
    void getUserIdByUsername_found_returnsId() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        UUID id = userService.getUserIdByUsername("testuser");

        assertEquals(user.getId(), id);
    }

    @Test
    void getUserIdByUsername_notFound_throws() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> userService.getUserIdByUsername("ghost"));
    }

    // ── deleteProfile ───────────────────────────────────────────────────────

    @Test
    void deleteProfile_found_deletesSessionsAndUser() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        userService.deleteProfile("testuser");

        verify(sessionRepository).deleteAllByUserId(user.getId());
        verify(userRepository).delete(user);
    }

    @Test
    void deleteProfile_notFound_throws() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> userService.deleteProfile("ghost"));
    }
}
