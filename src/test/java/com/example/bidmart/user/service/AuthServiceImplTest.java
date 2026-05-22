package com.example.bidmart.user.service;

import com.example.bidmart.common.event.UserRegisteredEvent;
import com.example.bidmart.user.dto.AuthResponse;
import com.example.bidmart.user.dto.LoginRequest;
import com.example.bidmart.user.dto.MfaVerificationRequest;
import com.example.bidmart.user.dto.RegisterRequest;
import com.example.bidmart.user.model.Role;
import com.example.bidmart.user.model.Session;
import com.example.bidmart.user.model.User;
import com.example.bidmart.user.repository.RoleRepository;
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
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private SessionRepository sessionRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private SessionService sessionService;
    @Mock
    private MfaService mfaService;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private AuthServiceImpl authService;

    private User user;
    private Role role;

    @BeforeEach
    void setUp() {
        role = new Role(UUID.randomUUID(), "USER", null);
        
        user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("testuser");
        user.setEmail("test@test.com");
        user.setPassword("encodedPassword");
        user.setRole(role);
        user.setActive(true);
        user.setEmailVerified(true);
    }

    @Test
    void register_validRequest_registersUser() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setEmail("new@new.com");
        request.setPassword("password");
        request.setRole("USER");

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@new.com")).thenReturn(false);
        when(roleRepository.findByName("USER")).thenReturn(Optional.of(role));
        when(passwordEncoder.encode("password")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("testuser", response.getUsername());
        verify(eventPublisher).publishEvent(any(UserRegisteredEvent.class));
    }

    @Test
    void register_usernameExists_throwsException() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("existing");

        when(userRepository.existsByUsername("existing")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> authService.register(request));
    }

    @Test
    void login_validCredentials_returnsAuthResponse() {
        LoginRequest request = new LoginRequest();
        request.setIdentifier("test@test.com");
        request.setPassword("password");

        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", "encodedPassword")).thenReturn(true);
        when(jwtService.generateAccessToken(user)).thenReturn("accessToken");
        when(jwtService.generateRefreshToken(user)).thenReturn("refreshToken");

        AuthResponse response = authService.login(request, "deviceInfo");

        assertNotNull(response);
        assertEquals("accessToken", response.getAccessToken());
        assertFalse(response.isMfaRequired());
        verify(sessionService).createSession(eq(user), eq("refreshToken"), eq("deviceInfo"));
    }

    @Test
    void login_requiresMfa_returnsTempToken() {
        user.setMfaEnabled(true);
        LoginRequest request = new LoginRequest();
        request.setIdentifier("testuser");
        request.setPassword("password");

        when(userRepository.findByEmail("testuser")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", "encodedPassword")).thenReturn(true);
        when(jwtService.generateTempToken(user)).thenReturn("tempToken");

        AuthResponse response = authService.login(request, "deviceInfo");

        assertTrue(response.isMfaRequired());
        assertEquals("tempToken", response.getTempToken());
    }

    @Test
    void verifyMfaLogin_validCode_returnsAuthResponse() {
        MfaVerificationRequest request = new MfaVerificationRequest();
        request.setTempToken("tempToken");
        request.setCode("123456");
        
        user.setMfaSecret("secret");

        when(jwtService.extractUsername("tempToken")).thenReturn("testuser");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(mfaService.verifyCode("secret", "123456")).thenReturn(true);
        when(jwtService.generateAccessToken(user)).thenReturn("accessToken");

        AuthResponse response = authService.verifyMfaLogin(request);

        assertNotNull(response);
        assertEquals("accessToken", response.getAccessToken());
    }

    @Test
    void verifyEmail_validToken_verifiesUser() {
        when(userRepository.findByVerificationToken("token")).thenReturn(Optional.of(user));

        boolean result = authService.verifyEmail("token");

        assertTrue(result);
        assertTrue(user.isEmailVerified());
        assertNull(user.getVerificationToken());
        verify(userRepository).save(user);
    }

    @Test
    void refreshToken_validToken_returnsNewTokens() {
        Session session = Session.builder()
                .user(user)
                .isRevoked(false)
                .expiresAt(Instant.now().plus(1, ChronoUnit.DAYS))
                .deviceInfo("device")
                .build();

        when(sessionRepository.findByRefreshToken("oldToken")).thenReturn(Optional.of(session));
        when(jwtService.generateAccessToken(user)).thenReturn("newAccess");
        when(jwtService.generateRefreshToken(user)).thenReturn("newRefresh");

        AuthResponse response = authService.refreshToken("oldToken");

        assertNotNull(response);
        assertEquals("newAccess", response.getAccessToken());
        assertTrue(session.isRevoked());
        verify(sessionRepository).save(session);
    }
}
