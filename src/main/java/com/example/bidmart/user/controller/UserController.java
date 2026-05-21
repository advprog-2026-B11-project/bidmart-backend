package com.example.bidmart.user.controller;

import com.example.bidmart.user.dto.UpdateProfileRequest;
import com.example.bidmart.user.dto.UserProfileResponse;
import com.example.bidmart.user.model.User;
import com.example.bidmart.user.repository.UserRepository;
import com.example.bidmart.user.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.HashMap;

//Handles user profile HTTP requests (read / update).
@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;

    public UserController(UserService userService, UserRepository userRepository) {
        this.userService = userService;
        this.userRepository = userRepository;
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

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteProfile(Authentication authentication) {
        userService.deleteProfile(authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me/mfa")
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
