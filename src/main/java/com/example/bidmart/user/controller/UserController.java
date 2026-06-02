package com.example.bidmart.user.controller;

import com.example.bidmart.user.dto.MfaDisableRequest;
import com.example.bidmart.user.dto.MfaEmailVerifyRequest;
import com.example.bidmart.user.dto.MfaEnableRequest;
import com.example.bidmart.user.dto.MfaSetupResponse;
import com.example.bidmart.user.dto.MfaStatusResponse;
import com.example.bidmart.user.dto.UpdateProfileRequest;
import com.example.bidmart.user.dto.UserProfileResponse;
import com.example.bidmart.user.model.User;
import com.example.bidmart.user.repository.UserRepository;
import com.example.bidmart.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.HashMap;

//Handles user profile HTTP requests (read / update).
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;

    public UserController(UserService userService, UserRepository userRepository) {
        this.userService = userService;
        this.userRepository = userRepository;
    }

    @GetMapping("/me")
    @PreAuthorize("hasAuthority(T(com.example.bidmart.common.security.PermissionNames).USER_READ)")
    public ResponseEntity<UserProfileResponse> getCurrentUser(Authentication authentication) {
        UserProfileResponse profile = userService.getCurrentUser(authentication.getName());
        return ResponseEntity.ok(profile);
    }

    @PutMapping("/me")
    @PreAuthorize("hasAuthority(T(com.example.bidmart.common.security.PermissionNames).USER_UPDATE)")
    public ResponseEntity<UserProfileResponse> updateProfile(
            Authentication authentication,
            @RequestBody UpdateProfileRequest request) {
        UserProfileResponse profile = userService.updateProfile(authentication.getName(), request);
        return ResponseEntity.ok(profile);
    }

    @GetMapping("/me/mfa")
    @PreAuthorize("hasAuthority(T(com.example.bidmart.common.security.PermissionNames).USER_MFA_READ)")
    public ResponseEntity<MfaStatusResponse> getMfaStatus(Authentication authentication) {
        MfaStatusResponse status = userService.getMfaStatus(authentication.getName());
        return ResponseEntity.ok(status);
    }

    @PostMapping("/me/mfa/setup")
    @PreAuthorize("hasAuthority(T(com.example.bidmart.common.security.PermissionNames).USER_MFA_SETUP)")
    public ResponseEntity<MfaSetupResponse> setupMfa(Authentication authentication) {
        MfaSetupResponse response = userService.setupMfa(authentication.getName());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/me/mfa/enable")
    @PreAuthorize("hasAuthority(T(com.example.bidmart.common.security.PermissionNames).USER_MFA_ENABLE)")
    public ResponseEntity<MfaStatusResponse> enableMfa(
            Authentication authentication,
            @Valid @RequestBody MfaEnableRequest request) {
        MfaStatusResponse status = userService.enableMfa(authentication.getName(), request.getCode());
        return ResponseEntity.ok(status);
    }

    @PostMapping("/me/mfa/email/enable")
    @PreAuthorize("hasAuthority(T(com.example.bidmart.common.security.PermissionNames).USER_MFA_ENABLE)")
    public ResponseEntity<MfaStatusResponse> enableEmailMfa(Authentication authentication) {
        MfaStatusResponse status = userService.enableEmailMfa(authentication.getName());
        return ResponseEntity.ok(status);
    }

    @PostMapping("/me/mfa/email/verify")
    @PreAuthorize("hasAuthority(T(com.example.bidmart.common.security.PermissionNames).USER_MFA_ENABLE)")
    public ResponseEntity<MfaStatusResponse> verifyEmailMfa(
            Authentication authentication,
            @Valid @RequestBody MfaEmailVerifyRequest request) {
        MfaStatusResponse status = userService.verifyEmailMfa(authentication.getName(), request.getCode());
        return ResponseEntity.ok(status);
    }

    @PostMapping("/me/mfa/disable")
    @PreAuthorize("hasAuthority(T(com.example.bidmart.common.security.PermissionNames).USER_MFA_DISABLE)")
    public ResponseEntity<MfaStatusResponse> disableMfa(
            Authentication authentication,
            @Valid @RequestBody MfaDisableRequest request) {
        MfaStatusResponse status = userService.disableMfa(
                authentication.getName(),
                request.getPassword(),
                request.getTotpCode());
        return ResponseEntity.ok(status);
    }

    @DeleteMapping("/me")
    @PreAuthorize("hasAuthority(T(com.example.bidmart.common.security.PermissionNames).USER_DELETE)")
    public ResponseEntity<Void> deleteProfile(Authentication authentication) {
        userService.deleteProfile(authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me/mfa/status")
    public ResponseEntity<Map<String, Boolean>> getMyMfaStatus(Authentication authentication) {
        String identifier = authentication.getName();

        User user = userRepository.findByEmail(identifier)
                .orElseGet(() -> userRepository.findByUsername(identifier)
                        .orElseThrow(() -> new RuntimeException("Data pengguna tidak ditemukan")));

        Map<String, Boolean> response = new HashMap<>();
        response.put("mfaEnabled", user.isMfaEnabled());
        return ResponseEntity.ok(response);
    }
}
    