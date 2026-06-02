package com.example.bidmart.user.service;

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
import com.example.bidmart.common.event.UserRegisteredEvent;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private SessionRepository sessionRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private SessionService sessionService;
    @Mock private MfaService mfaService;
    @Mock private RoleRepository roleRepository;
    @Mock private EmailService emailService;
    @Mock private ApplicationEventPublisher eventPublisher;

    private SimpleMeterRegistry meterRegistry;
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        authService = new AuthServiceImpl(
            userRepository, sessionRepository, passwordEncoder, jwtService,
            sessionService, mfaService, roleRepository, emailService, eventPublisher,
            meterRegistry, "http://example.com/verify/{token}", 300L, 3
        );
        ReflectionTestUtils.setField(authService, "authServiceRef", authService);
    }

    private User buildUser() {
        Role role = new Role();
        role.setName("USER");
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("alice");
        user.setEmail("alice@example.com");
        user.setDisplayName("Alice");
        user.setPassword("encoded");
        user.setRole(role);
        user.setActive(true);
        user.setEmailVerified(true);
        return user;
    }

    private SessionResponse buildSession() {
        return new SessionResponse(UUID.randomUUID(), "device", Instant.now(), Instant.now().plusSeconds(604800));
    }

    private LoginRequest loginReq(String id, String pw) {
        LoginRequest r = new LoginRequest();
        r.setIdentifier(id);
        r.setPassword(pw);
        return r;
    }

    private MfaVerificationRequest mfaReq(String token, String code) {
        MfaVerificationRequest r = new MfaVerificationRequest();
        r.setTempToken(token);
        r.setCode(code);
        return r;
    }

    // ─── register ───

    @Test
    void register_success() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("alice"); req.setEmail("alice@example.com");
        req.setDisplayName("Alice"); req.setPassword("password1"); req.setRole("USER");

        Role role = new Role(); role.setName("USER");
        User saved = buildUser();

        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password1")).thenReturn("encoded");
        when(roleRepository.findByName("USER")).thenReturn(Optional.of(role));
        when(userRepository.save(any(User.class))).thenReturn(saved);

        AuthResponse resp = authService.register(req);

        assertEquals("alice", resp.getUsername());
        assertNull(resp.getAccessToken());
        verify(emailService).sendVerificationEmail(eq("alice@example.com"), anyString());

        // Registration must publish UserRegisteredEvent so the wallet module
        // auto-provisions a wallet for the new user (buyer & seller alike).
        ArgumentCaptor<UserRegisteredEvent> eventCaptor = ArgumentCaptor.forClass(UserRegisteredEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertEquals(saved.getId(), eventCaptor.getValue().userId());
        assertEquals("alice", eventCaptor.getValue().username());
    }

    @Test
    void register_success_incrementsSuccessCounter() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("alice"); req.setEmail("alice@example.com");
        req.setDisplayName("Alice"); req.setPassword("password1"); req.setRole("USER");

        Role role = new Role(); role.setName("USER");
        User saved = buildUser();

        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password1")).thenReturn("encoded");
        when(roleRepository.findByName("USER")).thenReturn(Optional.of(role));
        when(userRepository.save(any(User.class))).thenReturn(saved);

        authService.register(req);

        assertEquals(1.0,
                meterRegistry.get("bidmart.auth.register").tag("result", "success").counter().count(),
                0.0001);
    }

    @Test
    void login_userNotFound_incrementsFailureCounter() {
        when(userRepository.findByEmail("ghost")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());
        LoginRequest req = loginReq("ghost", "password1");

        assertThrows(IllegalArgumentException.class,
                () -> authService.login(req, "device"));

        assertEquals(1.0,
                meterRegistry.get("bidmart.auth.login").tag("result", "failure").counter().count(),
                0.0001);
    }

    @Test
    void register_usernameAlreadyTaken_throws() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("alice"); req.setEmail("alice@example.com");
        req.setDisplayName("Alice"); req.setPassword("password1");

        when(userRepository.existsByUsername("alice")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> authService.register(req));
    }

    @Test
    void register_emailAlreadyRegistered_throws() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("alice"); req.setEmail("alice@example.com");
        req.setDisplayName("Alice"); req.setPassword("password1");

        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> authService.register(req));
    }

    @Test
    void register_nullRole_defaultsToBuyer() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("alice"); req.setEmail("alice@example.com");
        req.setDisplayName("Alice"); req.setPassword("password1"); req.setRole(null);

        Role role = new Role(); role.setName("USER");
        User saved = buildUser();

        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password1")).thenReturn("encoded");
        when(roleRepository.findByName("USER")).thenReturn(Optional.of(role));
        when(userRepository.save(any(User.class))).thenReturn(saved);

        AuthResponse resp = authService.register(req);
        assertEquals("USER", resp.getRole());
    }

    @Test
    void register_sellerRole_allowed() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("alice"); req.setEmail("alice@example.com");
        req.setDisplayName("Alice"); req.setPassword("password1"); req.setRole("SELLER");

        Role role = new Role(); role.setName("SELLER");
        User saved = buildUser(); saved.setRole(role);

        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password1")).thenReturn("encoded");
        when(roleRepository.findByName("SELLER")).thenReturn(Optional.of(role));
        when(userRepository.save(any(User.class))).thenReturn(saved);

        AuthResponse resp = authService.register(req);
        assertEquals("SELLER", resp.getRole());
    }

    @Test
    void register_invalidRole_throws() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("alice"); req.setEmail("alice@example.com");
        req.setDisplayName("Alice"); req.setPassword("password1"); req.setRole("ADMIN");

        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> authService.register(req));
    }

    @Test
    void register_urlWithoutPlaceholder_appendsToken() {
        authService = new AuthServiceImpl(
            userRepository, sessionRepository, passwordEncoder, jwtService,
            sessionService, mfaService, roleRepository, emailService, eventPublisher,
            meterRegistry, "http://example.com/verify?token=", 300L, 3
        );

        RegisterRequest req = new RegisterRequest();
        req.setUsername("alice"); req.setEmail("alice@example.com");
        req.setDisplayName("Alice"); req.setPassword("password1");

        Role role = new Role(); role.setName("USER");
        User saved = buildUser();

        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password1")).thenReturn("encoded");
        when(roleRepository.findByName("USER")).thenReturn(Optional.of(role));
        when(userRepository.save(any(User.class))).thenReturn(saved);

        authService.register(req);

        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendVerificationEmail(anyString(), urlCaptor.capture());
        assertTrue(urlCaptor.getValue().startsWith("http://example.com/verify?token="));
    }

    // ─── login ───

    @Test
    void login_success() {
        User user = buildUser();
        SessionResponse session = buildSession();

        when(userRepository.findByEmail("alice")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("rawPw", "encoded")).thenReturn(true);
        when(jwtService.generateRefreshToken(user)).thenReturn("refresh");
        when(sessionService.createSession(user, "refresh", "Chrome")).thenReturn(session);
        when(jwtService.generateAccessToken(eq(user), eq(session.getId()))).thenReturn("access");

        AuthResponse resp = authService.login(loginReq("alice", "rawPw"), "Chrome");

        assertEquals("access", resp.getAccessToken());
        assertFalse(resp.isMfaRequired());
    }

    @Test
    void login_deactivatedAccount_throws() {
        User user = buildUser(); user.setActive(false);

        when(userRepository.findByEmail("alice")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        assertThrows(IllegalArgumentException.class,
            () -> authService.login(loginReq("alice", "rawPw"), "device"));
    }

    @Test
    void login_invalidPassword_throws() {
        User user = buildUser();

        when(userRepository.findByEmail("alice")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "encoded")).thenReturn(false);

        assertThrows(IllegalArgumentException.class,
            () -> authService.login(loginReq("alice", "wrong"), "device"));
    }

    @Test
    void login_emailNotVerified_resendsThenThrows() {
        User user = buildUser();
        user.setEmailVerified(false);
        user.setVerificationToken("token123");

        when(userRepository.findByEmail("alice")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("rawPw", "encoded")).thenReturn(true);
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));

        assertThrows(IllegalArgumentException.class,
            () -> authService.login(loginReq("alice", "rawPw"), "device"));
        verify(emailService).sendVerificationEmail(anyString(), anyString());
    }

    @Test
    void login_mfaTotp_returnsMfaChallenge() {
        User user = buildUser();
        user.setMfaEnabled(true); user.setMfaMethod(MfaMethod.TOTP); user.setMfaSecret("secret");

        when(userRepository.findByEmail("alice")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("rawPw", "encoded")).thenReturn(true);
        when(jwtService.generateTempToken(user)).thenReturn("temp");

        AuthResponse resp = authService.login(loginReq("alice", "rawPw"), "device");

        assertTrue(resp.isMfaRequired());
        assertEquals("temp", resp.getTempToken());
    }

    @Test
    void login_mfaEmail_sendsCodeAndReturnsMfaChallenge() {
        User user = buildUser();
        user.setMfaEnabled(true); user.setMfaMethod(MfaMethod.EMAIL);

        when(userRepository.findByEmail("alice")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("rawPw", "encoded")).thenReturn(true);
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(jwtService.generateTempToken(user)).thenReturn("temp");

        AuthResponse resp = authService.login(loginReq("alice", "rawPw"), "device");

        assertTrue(resp.isMfaRequired());
        verify(emailService).sendMfaCodeEmail(eq("alice@example.com"), anyString());
    }

    @Test
    void login_nullDeviceInfo_usesUnknownDevice() {
        User user = buildUser();
        SessionResponse session = buildSession();

        when(userRepository.findByEmail("alice")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("rawPw", "encoded")).thenReturn(true);
        when(jwtService.generateRefreshToken(user)).thenReturn("refresh");
        when(sessionService.createSession(eq(user), eq("refresh"), eq("Unknown-Device"))).thenReturn(session);
        when(jwtService.generateAccessToken(eq(user), any())).thenReturn("access");

        authService.login(loginReq("alice", "rawPw"), null);

        verify(sessionService).createSession(user, "refresh", "Unknown-Device");
    }

    @Test
    void login_blankDeviceInfo_usesUnknownDevice() {
        User user = buildUser();
        SessionResponse session = buildSession();

        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("rawPw", "encoded")).thenReturn(true);
        when(jwtService.generateRefreshToken(user)).thenReturn("refresh");
        when(sessionService.createSession(eq(user), eq("refresh"), eq("Unknown-Device"))).thenReturn(session);
        when(jwtService.generateAccessToken(eq(user), any())).thenReturn("access");

        authService.login(loginReq("alice@example.com", "rawPw"), "   ");

        verify(sessionService).createSession(user, "refresh", "Unknown-Device");
    }

    @Test
    void login_userNotFound_throws() {
        when(userRepository.findByEmail("nobody")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("nobody")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
            () -> authService.login(loginReq("nobody", "pw"), "device"));
    }

    // ─── verifyMfaLogin ───

    @Test
    void verifyMfaLogin_totp_success() {
        User user = buildUser();
        user.setMfaMethod(MfaMethod.TOTP); user.setMfaSecret("secret");
        SessionResponse session = buildSession();

        when(jwtService.extractUsername("temp")).thenReturn("alice");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(mfaService.verifyCode("secret", "123456")).thenReturn(true);
        when(jwtService.generateRefreshToken(user)).thenReturn("refresh");
        when(sessionService.createSession(eq(user), eq("refresh"), eq("Default Device"))).thenReturn(session);
        when(jwtService.generateAccessToken(eq(user), any())).thenReturn("access");

        AuthResponse resp = authService.verifyMfaLogin(mfaReq("temp", "123456"));

        assertFalse(resp.isMfaRequired());
        assertEquals("access", resp.getAccessToken());
    }

    @Test
    void verifyMfaLogin_totp_invalidCode_throws() {
        User user = buildUser();
        user.setMfaMethod(MfaMethod.TOTP); user.setMfaSecret("secret");

        when(jwtService.extractUsername("temp")).thenReturn("alice");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(mfaService.verifyCode("secret", "wrong")).thenReturn(false);

        assertThrows(IllegalArgumentException.class,
            () -> authService.verifyMfaLogin(mfaReq("temp", "wrong")));
    }

    @Test
    void verifyMfaLogin_email_success() {
        User user = buildUser();
        user.setMfaMethod(MfaMethod.EMAIL);
        user.setMfaEmailCode("654321");
        user.setMfaEmailCodeExpiresAt(Instant.now().plusSeconds(300));
        SessionResponse session = buildSession();

        when(jwtService.extractUsername("temp")).thenReturn("alice");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(jwtService.generateRefreshToken(user)).thenReturn("refresh");
        when(sessionService.createSession(eq(user), eq("refresh"), eq("Default Device"))).thenReturn(session);
        when(jwtService.generateAccessToken(eq(user), any())).thenReturn("access");

        AuthResponse resp = authService.verifyMfaLogin(mfaReq("temp", "654321"));

        assertFalse(resp.isMfaRequired());
        assertNull(user.getMfaEmailCode());
    }

    @Test
    void verifyMfaLogin_email_expiredCode_throws() {
        User user = buildUser();
        user.setMfaMethod(MfaMethod.EMAIL);
        user.setMfaEmailCode("654321");
        user.setMfaEmailCodeExpiresAt(Instant.now().minusSeconds(1));

        when(jwtService.extractUsername("temp")).thenReturn("alice");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        assertThrows(IllegalArgumentException.class,
            () -> authService.verifyMfaLogin(mfaReq("temp", "654321")));
    }

    @Test
    void verifyMfaLogin_email_nullCode_throws() {
        User user = buildUser();
        user.setMfaMethod(MfaMethod.EMAIL);
        user.setMfaEmailCode(null);

        when(jwtService.extractUsername("temp")).thenReturn("alice");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        assertThrows(IllegalArgumentException.class,
            () -> authService.verifyMfaLogin(mfaReq("temp", "654321")));
    }

    @Test
    void verifyMfaLogin_email_wrongCode_throws() {
        User user = buildUser();
        user.setMfaMethod(MfaMethod.EMAIL);
        user.setMfaEmailCode("654321");
        user.setMfaEmailCodeExpiresAt(Instant.now().plusSeconds(300));

        when(jwtService.extractUsername("temp")).thenReturn("alice");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        assertThrows(IllegalArgumentException.class,
            () -> authService.verifyMfaLogin(mfaReq("temp", "000000")));
    }

    @Test
    void verifyMfaLogin_deactivatedUser_throws() {
        User user = buildUser(); user.setActive(false);

        when(jwtService.extractUsername("temp")).thenReturn("alice");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        assertThrows(IllegalArgumentException.class,
            () -> authService.verifyMfaLogin(mfaReq("temp", "123456")));
    }

    @Test
    void verifyMfaLogin_nullMfaMethod_defaultsToTotp() {
        User user = buildUser();
        user.setMfaMethod(null); user.setMfaSecret("secret");
        SessionResponse session = buildSession();

        when(jwtService.extractUsername("temp")).thenReturn("alice");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(mfaService.verifyCode("secret", "123456")).thenReturn(true);
        when(jwtService.generateRefreshToken(user)).thenReturn("refresh");
        when(sessionService.createSession(eq(user), eq("refresh"), eq("Default Device"))).thenReturn(session);
        when(jwtService.generateAccessToken(eq(user), any())).thenReturn("access");

        assertFalse(authService.verifyMfaLogin(mfaReq("temp", "123456")).isMfaRequired());
    }

    // ─── finalizeLogin ───

    @Test
    void finalizeLogin_success() {
        User user = buildUser();
        SessionResponse session = buildSession();

        when(jwtService.generateRefreshToken(user)).thenReturn("refresh");
        when(sessionService.createSession(eq(user), eq("refresh"), eq("myDevice"))).thenReturn(session);
        when(jwtService.generateAccessToken(eq(user), eq(session.getId()))).thenReturn("access");

        AuthResponse resp = authService.finalizeLogin(user, "myDevice");

        assertEquals("access", resp.getAccessToken());
        assertEquals("refresh", resp.getRefreshToken());
        assertFalse(resp.isMfaRequired());
    }

    // ─── verifyEmail ───

    @Test
    void verifyEmail_validToken_returnsTrue() {
        User user = buildUser();
        user.setEmailVerified(false);
        user.setVerificationToken("valid-token");

        when(userRepository.findByVerificationToken("valid-token")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        assertTrue(authService.verifyEmail("valid-token"));
        assertTrue(user.isEmailVerified());
        assertNull(user.getVerificationToken());
    }

    @Test
    void verifyEmail_invalidToken_returnsFalse() {
        when(userRepository.findByVerificationToken("bad")).thenReturn(Optional.empty());

        assertFalse(authService.verifyEmail("bad"));
    }

    // ─── resendVerification ───

    @Test
    void resendVerification_existingToken_sendWithoutSaving() {
        User user = buildUser();
        user.setEmailVerified(false);
        user.setVerificationToken("existing-token");

        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));

        authService.resendVerification("alice@example.com");

        verify(emailService).sendVerificationEmail(anyString(), contains("existing-token"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void resendVerification_nullToken_generatesNew() {
        User user = buildUser();
        user.setEmailVerified(false);
        user.setVerificationToken(null);

        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        authService.resendVerification("alice@example.com");

        verify(userRepository).save(user);
        verify(emailService).sendVerificationEmail(anyString(), anyString());
    }

    @Test
    void resendVerification_blankToken_generatesNew() {
        User user = buildUser();
        user.setEmailVerified(false);
        user.setVerificationToken("   ");

        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        authService.resendVerification("alice@example.com");

        verify(userRepository).save(user);
    }

    @Test
    void resendVerification_alreadyVerified_throws() {
        User user = buildUser(); // emailVerified = true

        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));

        assertThrows(IllegalArgumentException.class,
            () -> authService.resendVerification("alice@example.com"));
    }

    // ─── refreshToken ───

    @Test
    void refreshToken_success() {
        User user = buildUser();
        Session session = Session.builder()
            .id(UUID.randomUUID()).user(user).refreshToken("old-refresh")
            .expiresAt(Instant.now().plusSeconds(3600)).isRevoked(false).deviceInfo("device")
            .build();
        SessionResponse newSession = buildSession();

        when(sessionRepository.findByRefreshToken("old-refresh")).thenReturn(Optional.of(session));
        when(sessionRepository.save(any(Session.class))).thenReturn(session);
        when(jwtService.generateRefreshToken(user)).thenReturn("new-refresh");
        when(sessionService.createSession(user, "new-refresh", "device")).thenReturn(newSession);
        when(jwtService.generateAccessToken(eq(user), eq(newSession.getId()))).thenReturn("new-access");

        AuthResponse resp = authService.refreshToken("old-refresh");

        assertEquals("new-access", resp.getAccessToken());
        assertEquals("new-refresh", resp.getRefreshToken());
        assertTrue(session.isRevoked());
    }

    @Test
    void refreshToken_invalidToken_throws() {
        when(sessionRepository.findByRefreshToken("bad")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> authService.refreshToken("bad"));
    }

    @Test
    void refreshToken_revokedSession_throws() {
        User user = buildUser();
        Session session = Session.builder().user(user).refreshToken("revoked")
            .expiresAt(Instant.now().plusSeconds(3600)).isRevoked(true).build();

        when(sessionRepository.findByRefreshToken("revoked")).thenReturn(Optional.of(session));

        assertThrows(IllegalArgumentException.class, () -> authService.refreshToken("revoked"));
    }

    @Test
    void refreshToken_expiredSession_throws() {
        User user = buildUser();
        Session session = Session.builder().user(user).refreshToken("expired")
            .expiresAt(Instant.now().minusSeconds(1)).isRevoked(false).build();

        when(sessionRepository.findByRefreshToken("expired")).thenReturn(Optional.of(session));

        assertThrows(IllegalArgumentException.class, () -> authService.refreshToken("expired"));
    }

    @Test
    void refreshToken_deactivatedUser_throws() {
        User user = buildUser(); user.setActive(false);
        Session session = Session.builder().user(user).refreshToken("refresh")
            .expiresAt(Instant.now().plusSeconds(3600)).isRevoked(false).build();

        when(sessionRepository.findByRefreshToken("refresh")).thenReturn(Optional.of(session));

        assertThrows(IllegalArgumentException.class, () -> authService.refreshToken("refresh"));
    }
}
