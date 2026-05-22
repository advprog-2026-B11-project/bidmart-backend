package com.example.bidmart.user.service;

import com.example.bidmart.common.event.UserRegisteredEvent;
import com.example.bidmart.user.dto.AuthResponse;
import com.example.bidmart.user.dto.LoginRequest;
import com.example.bidmart.user.dto.MfaVerificationRequest;
import com.example.bidmart.user.dto.RegisterRequest;
import com.example.bidmart.user.dto.SessionResponse;
import com.example.bidmart.user.model.MfaMethod;
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
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
    private EmailService emailService;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private AuthServiceImpl authService;

    private User user;
    private Role role;

    @BeforeEach
    void setUp() {
        role = new Role(UUID.randomUUID(), "USER", new HashSet<>());

        user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("testuser");
        user.setEmail("test@mail.com");
        user.setPassword("encoded-password");
        user.setRole(role);
        user.setDisplayName("Test User");
        user.setEmailVerified(true);
        user.setActive(true);

        authService = new AuthServiceImpl(
                userRepository,
                sessionRepository,
                passwordEncoder,
                jwtService,
                sessionService,
                mfaService,
                roleRepository,
                emailService,
                "http://localhost:8080/api/auth/verify?token={token}",
                300L,
                3
        );
    }

    @Test
    void register_shouldSaveUserAndReturnResponse() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setEmail("new@new.com");
        request.setPassword("password");
        request.setRole("USER");

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@new.com")).thenReturn(false);
        when(roleRepository.findByName("USER")).thenReturn(Optional.of(role));
        when(passwordEncoder.encode("password")).thenReturn("encodedPassword");
        
        User savedUser = new User();
        savedUser.setUsername("newuser");
        savedUser.setEmail("new@new.com");
        savedUser.setRole(role);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("newuser", response.getUsername());
        verify(userRepository, times(1)).save(any(User.class));
        verify(emailService, times(1)).sendVerificationEmail(eq("new@new.com"), anyString());
    }

    @Test
    void register_shouldUseRequestedRoleWhenProvided() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("selleruser");
        request.setEmail("seller@mail.com");
        request.setPassword("password123");
        request.setDisplayName("Seller User");
        request.setRole("seller");

        Role sellerRole = new Role(UUID.randomUUID(), "SELLER", new HashSet<>());

        when(userRepository.existsByUsername(request.getUsername())).thenReturn(false);
        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encoded-password");
        when(roleRepository.findByName("SELLER")).thenReturn(Optional.of(sellerRole));

        User savedUser = new User();
        savedUser.setUsername(request.getUsername());
        savedUser.setEmail(request.getEmail());
        savedUser.setRole(sellerRole);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("SELLER", response.getRole());
        verify(roleRepository, times(1)).findByName("SELLER");
        verify(emailService, times(1)).sendVerificationEmail(eq("seller@mail.com"), anyString());
    }

    @Test
    void resendVerification_shouldRotateTokenAndSendEmail() {
        user.setEmailVerified(false);
        user.setVerificationToken("old-token");

        when(userRepository.findByEmail("test@mail.com")).thenReturn(Optional.of(user));

        authService.resendVerification("test@mail.com");

        assertNotNull(user.getVerificationToken());
        verify(userRepository, times(1)).save(user);
        verify(emailService, times(1)).sendVerificationEmail(eq("test@mail.com"), anyString());
    }

    @Test
    void resendVerification_shouldThrowWhenAlreadyVerified() {
        user.setEmailVerified(true);

        when(userRepository.findByEmail("test@mail.com")).thenReturn(Optional.of(user));

        assertThrows(IllegalArgumentException.class, () -> authService.resendVerification("test@mail.com"));
        verify(emailService, never()).sendVerificationEmail(anyString(), anyString());
    }

    @Test
    void register_usernameExists_throwsException() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("existing");

        when(userRepository.existsByUsername("existing")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> authService.register(request));
    }

    @Test
    void register_emailExists_throwsException() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setEmail("existing@test.com");

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("existing@test.com")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> authService.register(request));
    }

    @Test
    void register_nullRole_defaultsToUser() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setEmail("new@new.com");
        request.setPassword("password");
        request.setRole(null);

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@new.com")).thenReturn(false);
        when(roleRepository.findByName("USER")).thenReturn(Optional.of(role));
        when(passwordEncoder.encode("password")).thenReturn("encodedPassword");
        
        User savedUser = new User();
        savedUser.setUsername("newuser");
        savedUser.setRole(role);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        AuthResponse response = authService.register(request);
        assertNotNull(response);
    }

    @Test
    void register_blankRole_defaultsToUser() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setEmail("new@new.com");
        request.setPassword("password");
        request.setRole("   ");

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@new.com")).thenReturn(false);
        when(roleRepository.findByName("USER")).thenReturn(Optional.of(role));
        when(passwordEncoder.encode("password")).thenReturn("encodedPassword");
        
        User savedUser = new User();
        savedUser.setUsername("newuser");
        savedUser.setRole(role);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        AuthResponse response = authService.register(request);
        assertNotNull(response);
    }

    @Test
    void register_sellerRole_success() {
        Role sellerRole = new Role(UUID.randomUUID(), "SELLER", null);
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setEmail("new@new.com");
        request.setPassword("password");
        request.setRole("SELLER");

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@new.com")).thenReturn(false);
        when(roleRepository.findByName("SELLER")).thenReturn(Optional.of(sellerRole));
        when(passwordEncoder.encode("password")).thenReturn("encodedPassword");
        
        User savedUser = new User();
        savedUser.setUsername("newuser");
        savedUser.setRole(sellerRole);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        AuthResponse response = authService.register(request);
        assertNotNull(response);
    }

    @Test
    void register_invalidRole_throwsException() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setEmail("new@new.com");
        request.setRole("ADMIN");

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@new.com")).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> authService.register(request));
    }

    @Test
    void register_roleNotFoundInDb_throwsException() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setEmail("new@new.com");
        request.setPassword("password");
        request.setRole("SELLER");

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@new.com")).thenReturn(false);
        when(roleRepository.findByName("SELLER")).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> authService.register(request));
    }

    @Test
    void login_validCredentials_returnsAuthResponse() {
        LoginRequest request = new LoginRequest();
        request.setIdentifier("testuser");
        request.setPassword("password123");

        when(userRepository.findByEmail("testuser")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "encoded-password")).thenReturn(true);
        when(jwtService.generateRefreshToken(user)).thenReturn("refreshToken");
        UUID sessionId = UUID.randomUUID();
        SessionResponse sessionResponse = SessionResponse.builder()
            .id(sessionId)
            .deviceInfo("Test Device")
            .createdAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
            .build();
        when(sessionService.createSession(user, "refreshToken", "deviceInfo")).thenReturn(sessionResponse);
        when(jwtService.generateAccessToken(user, sessionId)).thenReturn("accessToken");

        AuthResponse response = authService.login(request, "deviceInfo");

        assertNotNull(response);
        assertEquals("accessToken", response.getAccessToken());
        assertFalse(response.isMfaRequired());
        verify(sessionService).createSession(eq(user), eq("refreshToken"), eq("deviceInfo"));
    }

    @Test
    void login_identifierIsUsername_returnsAuthResponse() {
        LoginRequest request = new LoginRequest();
        request.setIdentifier("testuser");
        request.setPassword("password123");
        user.setMfaMethod(MfaMethod.TOTP);

        when(userRepository.findByEmail("testuser")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "encoded-password")).thenReturn(true);
        
        UUID sessionId = UUID.randomUUID();
        SessionResponse sessionResponse = SessionResponse.builder().id(sessionId).deviceInfo("Unknown-Device").build();
        
        when(jwtService.generateRefreshToken(user)).thenReturn("refreshToken");
        when(sessionService.createSession(user, "refreshToken", "Unknown-Device")).thenReturn(sessionResponse);
        when(jwtService.generateAccessToken(user, sessionId)).thenReturn("accessToken");

        AuthResponse response = authService.login(request, null);

        assertNotNull(response);
        verify(sessionService).createSession(eq(user), eq("refreshToken"), eq("Unknown-Device"));
    }

    @Test
    void login_blankDeviceInfo_defaultsToUnknown() {
        LoginRequest request = new LoginRequest();
        request.setIdentifier("test@mail.com");
        request.setPassword("password123");

        when(userRepository.findByEmail("test@mail.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "encoded-password")).thenReturn(true);
        
        UUID sessionId = UUID.randomUUID();
        SessionResponse sessionResponse = SessionResponse.builder().id(sessionId).deviceInfo("Unknown-Device").build();
        
        when(jwtService.generateRefreshToken(user)).thenReturn("refreshToken");
        when(sessionService.createSession(user, "refreshToken", "Unknown-Device")).thenReturn(sessionResponse);
        when(jwtService.generateAccessToken(user, sessionId)).thenReturn("accessToken");

        AuthResponse response = authService.login(request, "   ");

        assertNotNull(response);
        verify(sessionService).createSession(eq(user), eq("refreshToken"), eq("Unknown-Device"));
    }

    @Test
    void login_userNotFound_throwsException() {
        LoginRequest request = new LoginRequest();
        request.setIdentifier("unknown");
        when(userRepository.findByEmail("unknown")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> authService.login(request, "device"));
    }

    @Test
    void login_shouldSendEmailCodeWhenEmailMfaEnabled() {
        LoginRequest request = new LoginRequest();
        request.setIdentifier("testuser");
        request.setPassword("password123");
        user.setMfaEnabled(true);
        user.setMfaMethod(MfaMethod.EMAIL);

        when(userRepository.findByEmail("testuser")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "encoded-password")).thenReturn(true);
        when(jwtService.generateTempToken(user)).thenReturn("temp-token");

        AuthResponse response = authService.login(request, "Test Device");

        assertTrue(response.isMfaRequired());
        assertEquals("temp-token", response.getTempToken());
        assertNotNull(user.getMfaEmailCode());
        assertNotNull(user.getMfaEmailCodeExpiresAt());
        verify(emailService, times(1)).sendMfaCodeEmail(eq("test@mail.com"), anyString());
    }

    @Test
    void login_shouldThrowExceptionOnWrongPassword() {
        LoginRequest request = new LoginRequest();
        request.setIdentifier("testuser");
        request.setPassword("wrong-password");

        when(userRepository.findByEmail("testuser")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "encoded-password")).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> authService.login(request, "deviceInfo"));
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
        
        UUID sessionId = UUID.randomUUID();
        SessionResponse sessionResponse = SessionResponse.builder().id(sessionId).deviceInfo("Default Device").build();
        when(jwtService.generateRefreshToken(user)).thenReturn("refreshToken");
        when(sessionService.createSession(user, "refreshToken", "Default Device")).thenReturn(sessionResponse);
        when(jwtService.generateAccessToken(user, sessionId)).thenReturn("accessToken");

        AuthResponse response = authService.verifyMfaLogin(request);

        assertNotNull(response);
        assertEquals("accessToken", response.getAccessToken());
    }

    @Test
    void verifyMfaLogin_userNotFound_throwsException() {
        MfaVerificationRequest request = new MfaVerificationRequest();
        request.setTempToken("tempToken");

        when(jwtService.extractUsername("tempToken")).thenReturn("nonexistent");
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> authService.verifyMfaLogin(request));
    }

    @Test
    void verifyMfaLogin_inactiveUser_throwsException() {
        user.setActive(false);
        MfaVerificationRequest request = new MfaVerificationRequest();
        request.setTempToken("tempToken");

        when(jwtService.extractUsername("tempToken")).thenReturn("testuser");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        assertThrows(IllegalArgumentException.class, () -> authService.verifyMfaLogin(request));
    }

    @Test
    void verifyMfaLogin_invalidCode_throwsException() {
        MfaVerificationRequest request = new MfaVerificationRequest();
        request.setTempToken("tempToken");
        request.setCode("wrong");
        user.setMfaSecret("secret");

        when(jwtService.extractUsername("tempToken")).thenReturn("testuser");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(mfaService.verifyCode("secret", "wrong")).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> authService.verifyMfaLogin(request));
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
    void verifyEmail_invalidToken_returnsFalse() {
        when(userRepository.findByVerificationToken("invalid")).thenReturn(Optional.empty());
        assertFalse(authService.verifyEmail("invalid"));
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
        when(jwtService.generateRefreshToken(user)).thenReturn("new-refresh-token");
        UUID newSessionId = UUID.randomUUID();
        SessionResponse newSession = SessionResponse.builder()
            .id(newSessionId)
            .deviceInfo("Test Device")
            .createdAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
            .build();
        when(sessionService.createSession(user, "new-refresh-token", "device")).thenReturn(newSession);
        when(jwtService.generateAccessToken(user, newSessionId)).thenReturn("new-access-token");

        AuthResponse response = authService.refreshToken("oldToken");

        assertNotNull(response);
        assertEquals("new-access-token", response.getAccessToken());
        assertTrue(session.isRevoked());
        verify(sessionRepository).save(session);
    }

    @Test
    void refreshToken_invalidToken_throwsException() {
        when(sessionRepository.findByRefreshToken("invalid")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> authService.refreshToken("invalid"));
    }

    @Test
    void refreshToken_revokedToken_throwsException() {
        Session session = Session.builder()
                .isRevoked(true)
                .expiresAt(Instant.now().plus(1, ChronoUnit.DAYS))
                .build();
        when(sessionRepository.findByRefreshToken("revoked")).thenReturn(Optional.of(session));

        assertThrows(IllegalArgumentException.class, () -> authService.refreshToken("revoked"));
    }

    @Test
    void refreshToken_expiredToken_throwsException() {
        Session session = Session.builder()
                .isRevoked(false)
                .expiresAt(Instant.now().minus(1, ChronoUnit.DAYS))
                .build();
        when(sessionRepository.findByRefreshToken("expired")).thenReturn(Optional.of(session));

        assertThrows(IllegalArgumentException.class, () -> authService.refreshToken("expired"));
    }
    
    @Test
    void verifyMfaLogin_totp_shouldSucceed() {
        MfaVerificationRequest request = new MfaVerificationRequest();
        request.setTempToken("temp-token");
        request.setCode("123456");

        user.setMfaEnabled(true);
        user.setMfaSecret("SECRET");
        user.setMfaMethod(MfaMethod.TOTP);

        when(jwtService.extractUsername("temp-token")).thenReturn("testuser");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(mfaService.verifyCode("SECRET", "123456")).thenReturn(true);
        
        UUID sessionId = UUID.randomUUID();
        SessionResponse sessionResponse = SessionResponse.builder().id(sessionId).deviceInfo("Dev").build();
        when(jwtService.generateRefreshToken(user)).thenReturn("refresh-token");
        when(sessionService.createSession(eq(user), eq("refresh-token"), anyString())).thenReturn(sessionResponse);
        when(jwtService.generateAccessToken(user, sessionId)).thenReturn("access-token");

        AuthResponse response = authService.verifyMfaLogin(request);

        assertNotNull(response);
        assertEquals("access-token", response.getAccessToken());
        verify(sessionService, times(1)).enforceSessionLimit(any(), anyInt());
    }

    @Test
    void verifyMfaLogin_email_shouldSucceedAndClearCode() {
        MfaVerificationRequest request = new MfaVerificationRequest();
        request.setTempToken("temp-token");
        request.setCode("123456");

        user.setMfaEnabled(true);
        user.setMfaMethod(MfaMethod.EMAIL);
        user.setMfaEmailCode("123456");
        user.setMfaEmailCodeExpiresAt(Instant.now().plusSeconds(300));

        when(jwtService.extractUsername("temp-token")).thenReturn("testuser");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        
        UUID sessionId = UUID.randomUUID();
        SessionResponse sessionResponse = SessionResponse.builder().id(sessionId).build();
        when(jwtService.generateRefreshToken(user)).thenReturn("refresh-token");
        when(sessionService.createSession(any(), any(), any())).thenReturn(sessionResponse);
        when(jwtService.generateAccessToken(user, sessionId)).thenReturn("access-token");

        AuthResponse response = authService.verifyMfaLogin(request);

        assertNotNull(response);
        assertNull(user.getMfaEmailCode()); 
        assertNull(user.getMfaEmailCodeExpiresAt());
    }
}