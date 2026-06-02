package com.example.bidmart.user.service;

import com.example.bidmart.common.event.UserDeactivatedEvent;
import com.example.bidmart.common.event.UserRoleChangedEvent;
import com.example.bidmart.user.dto.AdminUserResponse;
import com.example.bidmart.user.dto.MfaSetupResponse;
import com.example.bidmart.user.dto.MfaStatusResponse;
import com.example.bidmart.user.dto.UpdateProfileRequest;
import com.example.bidmart.user.dto.UserProfileResponse;
import com.example.bidmart.user.model.MfaMethod;
import com.example.bidmart.user.model.Role;
import com.example.bidmart.user.model.User;
import com.example.bidmart.user.repository.SessionRepository;
import com.example.bidmart.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserServiceImpl implements UserService {

    private static final String MFA_METHOD_NONE = "NONE";
    private static final int EMAIL_MFA_CODE_LENGTH = 6;
    private static final int EMAIL_MFA_CODE_MAX = 1_000_000;
    private static final String USER_NOT_FOUND_MSG = "User not found.";

    private final SecureRandom secureRandom = new SecureRandom();

    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final MfaService mfaService;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final long emailMfaCodeTtlSeconds;

    @Autowired
    public UserServiceImpl(UserRepository userRepository,
                           SessionRepository sessionRepository,
                           ApplicationEventPublisher eventPublisher,
                           MfaService mfaService,
                           PasswordEncoder passwordEncoder,
                           EmailService emailService,
                           @Value("${app.mfa.email-code-ttl-seconds:300}") long emailMfaCodeTtlSeconds) {
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.eventPublisher = eventPublisher;
        this.mfaService = mfaService;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.emailMfaCodeTtlSeconds = emailMfaCodeTtlSeconds;
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getCurrentUser(String username) {
        return mapToProfileResponse(findByUsername(username));
    }

    @Override
    @Transactional
    public UserProfileResponse updateProfile(String username, UpdateProfileRequest request) {
        User user = findByUsername(username);

        if (request.getDisplayName() != null && !request.getDisplayName().isBlank()) {
            user.setDisplayName(request.getDisplayName());
        }
        if (request.getPhoneNumber() != null && !request.getPhoneNumber().isBlank()) {
            user.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getImageUrl() != null && !request.getImageUrl().isBlank()) {
            user.setImageUrl(request.getImageUrl());
        }
        if (request.getShippingAddress() != null && !request.getShippingAddress().isBlank()) {
            user.setShippingAddress(request.getShippingAddress());
        }

        User savedUser = userRepository.save(user);
        return mapToProfileResponse(savedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public MfaStatusResponse getMfaStatus(String username) {
        User user = findByUsername(username);
        return mapToMfaStatus(user);
    }

    @Override
    @Transactional
    public MfaSetupResponse setupMfa(String username) {
        User user = findByUsername(username);

        String secret = mfaService.generateMfaSecret();
        user.setMfaSecret(secret);
        user.setMfaEnabled(false);
        user.setMfaMethod(MfaMethod.TOTP);
        userRepository.save(user);

        String qrCodeImageUri = mfaService.getQrCodeImageUri(secret, user.getEmail());
        return MfaSetupResponse.builder()
                .secret(secret)
                .qrCodeImageUri(qrCodeImageUri)
            .method(MfaMethod.TOTP.name())
                .enabled(false)
                .build();
    }

    @Override
    @Transactional
    public MfaStatusResponse enableMfa(String username, String code) {
        User user = findByUsername(username);
        if (user.getMfaSecret() == null || user.getMfaSecret().isBlank()) {
            throw new IllegalArgumentException("MFA is not configured. Please run setup first.");
        }
        if (!mfaService.verifyCode(user.getMfaSecret(), code)) {
            throw new IllegalArgumentException("Invalid 2FA Code.");
        }
        if (!user.isMfaEnabled()) {
            user.setMfaEnabled(true);
        }
        user.setMfaMethod(MfaMethod.TOTP);
        userRepository.save(user);
        return mapToMfaStatus(user);
    }

    @Override
    @Transactional
    public MfaStatusResponse enableEmailMfa(String username) {
        User user = findByUsername(username);
        if (user.isMfaEnabled()) {
            throw new IllegalArgumentException("MFA already enabled. Disable first to switch method.");
        }

        user.setMfaMethod(MfaMethod.EMAIL);
        issueEmailMfaCode(user);
        return mapToMfaStatus(user);
    }

    @Override
    @Transactional
    public MfaStatusResponse verifyEmailMfa(String username, String code) {
        User user = findByUsername(username);
        verifyEmailMfaCode(user, code);
        user.setMfaEnabled(true);
        user.setMfaMethod(MfaMethod.EMAIL);
        clearEmailMfaCode(user);
        userRepository.save(user);
        return mapToMfaStatus(user);
    }

    @Override
    @Transactional
    public MfaStatusResponse disableMfa(String username, String password, String totpCode) {
        User user = findByUsername(username);
        ensureReauthentication(user, password, totpCode);
        if (user.isMfaEnabled()) {
            user.setMfaEnabled(false);
            user.setMfaEmailCode(null);
            user.setMfaEmailCodeExpiresAt(null);
            userRepository.save(user);
        }
        return mapToMfaStatus(user);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdminUserResponse> listUsers(String search, String role, Pageable pageable) {
        String searchParam = (search == null || search.isBlank()) ? "" : search.trim();
        String roleParam = (role == null || role.isBlank()) ? "" : role.trim();
        return userRepository.findAllWithFilters(searchParam, roleParam, pageable)
                .map(AdminUserResponse::from);
    }

    @Override
    @Transactional
    public void reactivateUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException(USER_NOT_FOUND_MSG));
        if (user.isActive()) return;
        user.setActive(true);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void deactivateUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException(USER_NOT_FOUND_MSG));

        if (!user.isActive()) {
            return;
        }

        user.setActive(false);
        userRepository.save(user);
        sessionRepository.deleteAllByUserId(user.getId());

        eventPublisher.publishEvent(new UserDeactivatedEvent(
                user.getId(),
                user.getUsername(),
                Instant.now()
        ));
    }

    @Override
    @Transactional
    public void changeUserRole(UUID userId, Role newRole) {
        if (newRole == null) {
            throw new IllegalArgumentException("Role must be provided.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException(USER_NOT_FOUND_MSG));

        Role oldRole = user.getRole();
        if (oldRole == newRole) {
            return;
        }

        user.setRole(newRole);
        userRepository.save(user);

        eventPublisher.publishEvent(new UserRoleChangedEvent(
                user.getId(),
                oldRole.getName(),
                newRole.getName()
        ));
    }

    private UserProfileResponse mapToProfileResponse(User user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .phoneNumber(user.getPhoneNumber())
                .imageUrl(user.getImageUrl())
                .shippingAddress(user.getShippingAddress())
                .role(user.getRole().getName())
                .isEmailVerified(user.isEmailVerified())
                .build();
    }

    private MfaStatusResponse mapToMfaStatus(User user) {
        String method = user.getMfaMethod() == null
                ? MFA_METHOD_NONE
                : user.getMfaMethod().name();

        return MfaStatusResponse.builder()
                .enabled(user.isMfaEnabled())
                .method(method)
                .build();
    }

    private void ensureReauthentication(User user, String password, String totpCode) {
        boolean hasPassword = password != null && !password.isBlank();
        boolean hasTotpCode = totpCode != null && !totpCode.isBlank();

        if (!hasPassword && !hasTotpCode) {
            throw new IllegalArgumentException("Password or TOTP code is required.");
        }

        if (hasTotpCode) {
            if (user.getMfaSecret() == null || user.getMfaSecret().isBlank()) {
                throw new IllegalArgumentException("TOTP is not configured.");
            }
            if (!mfaService.verifyCode(user.getMfaSecret(), totpCode)) {
                throw new IllegalArgumentException("Invalid 2FA Code.");
            }
            return;
        }

        if (hasPassword && passwordEncoder.matches(password, user.getPassword())) {
            return;
        }

        throw new IllegalArgumentException("Invalid password or 2FA Code.");
    }

    private void issueEmailMfaCode(User user) {
        String code = generateEmailMfaCode();
        user.setMfaEmailCode(code);
        user.setMfaEmailCodeExpiresAt(Instant.now().plusSeconds(emailMfaCodeTtlSeconds));
        userRepository.save(user);
        emailService.sendMfaCodeEmail(user.getEmail(), code);
    }

    private void verifyEmailMfaCode(User user, String code) {
        if (user.getMfaEmailCode() == null || user.getMfaEmailCodeExpiresAt() == null) {
            throw new IllegalArgumentException("2FA code expired or not requested.");
        }
        if (user.getMfaEmailCodeExpiresAt().isBefore(Instant.now())) {
            clearEmailMfaCode(user);
            userRepository.save(user);
            throw new IllegalArgumentException("2FA code expired or not requested.");
        }
        if (!user.getMfaEmailCode().equals(code)) {
            throw new IllegalArgumentException("Invalid 2FA Code.");
        }
    }

    private void clearEmailMfaCode(User user) {
        user.setMfaEmailCode(null);
        user.setMfaEmailCodeExpiresAt(null);
    }

    private String generateEmailMfaCode() {
        int value = secureRandom.nextInt(EMAIL_MFA_CODE_MAX);
        return String.format("%0" + EMAIL_MFA_CODE_LENGTH + "d", value);
    }

    @Override
    @Transactional(readOnly = true)
    public UUID getUserIdByUsername(String username) {
        return findByUsername(username).getId();
    }

    private User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException(USER_NOT_FOUND_MSG));
    }

    @Override
    @Transactional
    public void deleteProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException(USER_NOT_FOUND_MSG));

        sessionRepository.deleteAllByUserId(user.getId());
        userRepository.delete(user);
    }
}
