package com.example.bidmart.user.controller;

import com.example.bidmart.user.dto.MfaDisableRequest;
import com.example.bidmart.user.dto.MfaEnableRequest;
import com.example.bidmart.user.dto.MfaSetupResponse;
import com.example.bidmart.user.dto.MfaStatusResponse;
import com.example.bidmart.user.dto.UpdateProfileRequest;
import com.example.bidmart.user.dto.UserProfileResponse;
import com.example.bidmart.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

//Handles user profile HTTP requests (read / update).
@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getCurrentUser(Authentication authentication) {
        UserProfileResponse profile = userService.getCurrentUser(authentication.getName());
        return ResponseEntity.ok(profile);
    }

    @PutMapping("/me")
    public ResponseEntity<UserProfileResponse> updateProfile(
            Authentication authentication,
            @RequestBody UpdateProfileRequest request) {
        UserProfileResponse profile = userService.updateProfile(authentication.getName(), request);
        return ResponseEntity.ok(profile);
    }

    @GetMapping("/me/mfa")
    public ResponseEntity<MfaStatusResponse> getMfaStatus(Authentication authentication) {
        MfaStatusResponse status = userService.getMfaStatus(authentication.getName());
        return ResponseEntity.ok(status);
    }

    @PostMapping("/me/mfa/setup")
    public ResponseEntity<MfaSetupResponse> setupMfa(Authentication authentication) {
        MfaSetupResponse response = userService.setupMfa(authentication.getName());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/me/mfa/enable")
    public ResponseEntity<MfaStatusResponse> enableMfa(
            Authentication authentication,
            @Valid @RequestBody MfaEnableRequest request) {
        MfaStatusResponse status = userService.enableMfa(authentication.getName(), request.getCode());
        return ResponseEntity.ok(status);
    }

    @PostMapping("/me/mfa/email/enable")
    public ResponseEntity<MfaStatusResponse> enableEmailMfa(Authentication authentication) {
        MfaStatusResponse status = userService.enableEmailMfa(authentication.getName());
        return ResponseEntity.ok(status);
    }

    @PostMapping("/me/mfa/disable")
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
    public ResponseEntity<Void> deleteProfile(Authentication authentication) {
        userService.deleteProfile(authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
