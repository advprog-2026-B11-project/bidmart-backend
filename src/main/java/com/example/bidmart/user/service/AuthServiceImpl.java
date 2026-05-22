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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class AuthServiceImpl implements AuthService {

    private static final int EMAIL_MFA_CODE_LENGTH = 6;
    private static final int EMAIL_MFA_CODE_MAX = 1_000_000;
    private static final long DEFAULT_EMAIL_MFA_TTL_SECONDS = 300L;
    private static final int DEFAULT_MAX_CONCURRENT_SESSIONS = 3;
    private static final String DEFAULT_ROLE_NAME = "BUYER";
    private static final Set<String> REGISTRABLE_ROLE_NAMES = Set.of("BUYER", "SELLER");

    private final SecureRandom secureRandom = new SecureRandom();

    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final SessionService sessionService;
    private final MfaService mfaService;
    private final RoleRepository roleRepository;
    private final EmailService emailService;
    private final String verificationUrlTemplate;
    private final long emailMfaCodeTtlSeconds;
    private final int maxConcurrentSessions;

    @Autowired
    public AuthServiceImpl(UserRepository userRepository,
                            SessionRepository sessionRepository,
                            PasswordEncoder passwordEncoder,
                            JwtService jwtService,
                            SessionService sessionService,
                            MfaService mfaService,
                            RoleRepository roleRepository,
                            EmailService emailService,
                            @Value("${app.email.verification-url-template}") String verificationUrlTemplate,
                            @Value("${app.mfa.email-code-ttl-seconds:300}") long emailMfaCodeTtlSeconds,
                            @Value("${app.session.max-concurrent:3}") int maxConcurrentSessions) {
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.sessionService = sessionService;
        this.mfaService = mfaService;
        this.roleRepository = roleRepository;
        this.emailService = emailService;
        this.verificationUrlTemplate = verificationUrlTemplate;
        this.emailMfaCodeTtlSeconds = emailMfaCodeTtlSeconds;
        this.maxConcurrentSessions = maxConcurrentSessions;
    }

    @Deprecated
    public AuthServiceImpl(UserRepository userRepository,
                           SessionRepository sessionRepository,
                           PasswordEncoder passwordEncoder,
                           JwtService jwtService,
                           SessionService sessionService,
                           MfaService mfaService,
                           RoleRepository roleRepository,
                           EmailService emailService,
                           String verificationUrlTemplate) {
        this(userRepository,
                sessionRepository,
                passwordEncoder,
                jwtService,
                sessionService,
                mfaService,
                roleRepository,
                emailService,
                verificationUrlTemplate,
                DEFAULT_EMAIL_MFA_TTL_SECONDS,
                DEFAULT_MAX_CONCURRENT_SESSIONS);
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        validateNewUser(request.getUsername(), request.getEmail());

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setDisplayName(request.getDisplayName());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        String roleName = resolveRoleName(request.getRole());
        Role selectedRole = roleRepository.findByName(roleName)
            .orElseThrow(() -> new IllegalStateException("Role '" + roleName + "' tidak ditemukan di database."));
        user.setRole(selectedRole);
        user.setEmailVerified(false);

        String verificationToken = UUID.randomUUID().toString();
        user.setVerificationToken(verificationToken);

        User savedUser = userRepository.save(user);

        sendVerificationEmail(savedUser, verificationToken);

        return mapToAuthResponse(savedUser, null, null);
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request, String deviceInfo) {
        User user = findUserByIdentifier(request.getIdentifier());

        if (!user.isActive()) {
            throw new IllegalArgumentException("Account is deactivated.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Invalid password.");
        }

        if (!user.isEmailVerified()) {
            resendVerification(user.getEmail()); 
            throw new IllegalArgumentException("Your email has not been verified yet. A new verification link has been sent to your email address.");
        }

        if (user.isMfaEnabled()){
            return startMfaChallenge(user);
        }

        String resolvedDeviceInfo = (deviceInfo == null || deviceInfo.isBlank())
                ? "Unknown-Device"
                : deviceInfo;
        return finalizeLogin(user, resolvedDeviceInfo);
    }
    @Override
    @Transactional
    public AuthResponse verifyMfaLogin(MfaVerificationRequest request){
        String username = jwtService.extractUsername(request.getTempToken());
        User user = userRepository.findByUsername(username).orElseThrow(() -> new IllegalArgumentException("User not found."));

        if (!user.isActive()) {
            throw new IllegalArgumentException("Account is deactivated.");
        }

        MfaMethod method = resolveMfaMethod(user);
        if (method == MfaMethod.EMAIL) {
            verifyEmailMfa(user, request.getCode());
        } else if (!mfaService.verifyCode(user.getMfaSecret(), request.getCode())){
            throw new IllegalArgumentException("Invalid 2FA Code.");
        }

        return finalizeLogin(user, "Default Device");
    }

    @Override
    @Transactional
    public AuthResponse finalizeLogin(User user, String deviceInfo){
        sessionService.enforceSessionLimit(user, maxConcurrentSessions);
        String refreshToken = jwtService.generateRefreshToken(user);
        SessionResponse session = sessionService.createSession(user, refreshToken, deviceInfo);
        String accessToken = jwtService.generateAccessToken(user, session.getId());

        return AuthResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .username(user.getUsername())
            .email(user.getEmail())
            .displayName(user.getDisplayName())
            .role(user.getRole().getName())
            .mfaRequired(false)
            .build();
    }

    @Override
    @Transactional
    public boolean verifyEmail(String token) {
        Optional<User> userOptional = userRepository.findByVerificationToken(token);
        if (userOptional.isEmpty()) {
            return false;
        }
        User user = userOptional.get();
        user.setEmailVerified(true);
        user.setVerificationToken(null);
        userRepository.save(user);
        return true;
    }

    @Override
    @Transactional
    public void resendVerification(String identifier) {
        User user = findUserByIdentifier(identifier);

        if (user.isEmailVerified()) {
            throw new IllegalArgumentException("Email already verified.");
        }

        String verificationToken = user.getVerificationToken();
        if (verificationToken == null || verificationToken.isBlank()) {
            verificationToken = UUID.randomUUID().toString();
            user.setVerificationToken(verificationToken);
            userRepository.save(user);
        }

        sendVerificationEmail(user, verificationToken);
    }

    private void validateNewUser(String username, String email) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username is already taken.");
        }
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email is already registered.");
        }
    }

    private String resolveRoleName(String requestedRole) {
        String roleName = (requestedRole == null || requestedRole.isBlank())
                ? DEFAULT_ROLE_NAME
                : requestedRole.trim().toUpperCase(Locale.ROOT);
        if (!REGISTRABLE_ROLE_NAMES.contains(roleName)) {
            throw new IllegalArgumentException("Role tidak valid. Pilih USER atau SELLER.");
        }
        return roleName;
    }

    private User findUserByIdentifier(String identifier) {
        Optional<User> userOptional = userRepository.findByEmail(identifier);

        if (userOptional.isEmpty()) {
            userOptional = userRepository.findByUsername(identifier);
        }

        return userOptional.orElseThrow(() ->
            new IllegalArgumentException("User with that identifier was not found.")
        );
    }

    private AuthResponse mapToAuthResponse(User user, String accessToken, String refreshToken) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .username(user.getUsername())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .role(user.getRole().getName())
                .build();
    }

    private void sendVerificationEmail(User user, String verificationToken) {
        String verificationUrl = buildVerificationUrl(verificationToken);
        emailService.sendVerificationEmail(user.getEmail(), verificationUrl);
    }

    private String buildVerificationUrl(String token) {
        if (verificationUrlTemplate.contains("{token}")) {
            return verificationUrlTemplate.replace("{token}", token);
        }
        return verificationUrlTemplate + token;
    }

    private AuthResponse startMfaChallenge(User user) {
        MfaMethod method = resolveMfaMethod(user);
        if (method == MfaMethod.EMAIL) {
            issueEmailMfaCode(user);
        }
        String tempToken = jwtService.generateTempToken(user);
        return AuthResponse.builder().mfaRequired(true).tempToken(tempToken).build();
    }

    private MfaMethod resolveMfaMethod(User user) {
        return user.getMfaMethod() == null ? MfaMethod.TOTP : user.getMfaMethod();
    }

    private void issueEmailMfaCode(User user) {
        String code = generateEmailMfaCode();
        user.setMfaEmailCode(code);
        user.setMfaEmailCodeExpiresAt(Instant.now().plusSeconds(emailMfaCodeTtlSeconds));
        userRepository.save(user);
        emailService.sendMfaCodeEmail(user.getEmail(), code);
    }

    private void verifyEmailMfa(User user, String code) {
        if (user.getMfaEmailCode() == null || user.getMfaEmailCodeExpiresAt() == null) {
            throw new IllegalArgumentException("2FA code expired or not requested.");
        }
        if (user.getMfaEmailCodeExpiresAt().isBefore(Instant.now())) {
            clearEmailMfaCode(user);
            throw new IllegalArgumentException("2FA code expired or not requested.");
        }
        if (!user.getMfaEmailCode().equals(code)) {
            throw new IllegalArgumentException("Invalid 2FA Code.");
        }
        clearEmailMfaCode(user);
    }

    private void clearEmailMfaCode(User user) {
        user.setMfaEmailCode(null);
        user.setMfaEmailCodeExpiresAt(null);
        userRepository.save(user);
    }

    private String generateEmailMfaCode() {
        int value = secureRandom.nextInt(EMAIL_MFA_CODE_MAX);
        return String.format("%0" + EMAIL_MFA_CODE_LENGTH + "d", value);
    }

    @Override
    public AuthResponse refreshToken(String refreshTokenStr) {
        Session session = sessionRepository.findByRefreshToken(refreshTokenStr).orElseThrow(() -> new IllegalArgumentException("Invalid refresh token."));
        if (session.isRevoked() || session.getExpiresAt().isBefore(Instant.now())){
            throw new IllegalArgumentException("Refresh token expired or revoked. Please login again.");
        }
        User user = session.getUser();
        if (!user.isActive()) {
            throw new IllegalArgumentException("User account is deactivated.");
        }

        session.setRevoked(true);
        sessionRepository.save(session);
        String newRefreshToken = jwtService.generateRefreshToken(user);
        SessionResponse newSession = sessionService.createSession(user, newRefreshToken, session.getDeviceInfo());
        String newAccessToken = jwtService.generateAccessToken(user, newSession.getId());

        return mapToAuthResponse(user, newAccessToken, newRefreshToken);
    }
}