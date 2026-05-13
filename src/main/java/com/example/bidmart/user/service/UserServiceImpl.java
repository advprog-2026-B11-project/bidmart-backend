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

import java.util.UUID;
import java.time.Instant;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserServiceImpl implements UserService {

    private static final String MFA_METHOD_NONE = "NONE";

    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final MfaService mfaService;

    public UserServiceImpl(UserRepository userRepository,
                           SessionRepository sessionRepository,
                           ApplicationEventPublisher eventPublisher,
                           MfaService mfaService) {
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.eventPublisher = eventPublisher;
        this.mfaService = mfaService;
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
        user.setMfaEnabled(true);
        user.setMfaMethod(MfaMethod.EMAIL);
        userRepository.save(user);
        return mapToMfaStatus(user);
    }

    @Override
    @Transactional
    public MfaStatusResponse disableMfa(String username) {
        User user = findByUsername(username);
        if (user.isMfaEnabled()) {
            user.setMfaEnabled(false);
            user.setMfaEmailCode(null);
            user.setMfaEmailCodeExpiresAt(null);
            userRepository.save(user);
        }
        return mapToMfaStatus(user);
    }

    @Override
    @Transactional
    public void deactivateUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

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
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

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

    @Override
    @Transactional(readOnly = true)
    public UUID getUserIdByUsername(String username) {
        return findByUsername(username).getId();
    }

    private User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));
    }

    @Override
    @Transactional
    public void deleteProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        sessionRepository.deleteAllByUserId(user.getId());
        userRepository.delete(user);
    }
}
