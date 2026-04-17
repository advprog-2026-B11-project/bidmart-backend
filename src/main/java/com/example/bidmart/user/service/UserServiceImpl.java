package com.example.bidmart.user.service;

import com.example.bidmart.user.dto.UpdateProfileRequest;
import com.example.bidmart.user.dto.UserProfileResponse;
import com.example.bidmart.user.model.User;
import com.example.bidmart.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getCurrentUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));
        return mapToProfileResponse(user);
    }

    @Override
    @Transactional
    public UserProfileResponse updateProfile(String username, UpdateProfileRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        if (request.getDisplayName() != null && !request.getDisplayName().isBlank()) {
            user.setDisplayName(request.getDisplayName());
        }
        if (request.getPhoneNumber() != null && !request.getPhoneNumber().isBlank()) {
            user.setPhoneNumber(request.getPhoneNumber());
        }

        User savedUser = userRepository.save(user);
        return mapToProfileResponse(savedUser);
    }

    private UserProfileResponse mapToProfileResponse(User user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole().name())
                .isEmailVerified(user.isEmailVerified())
                .build();
    }
}
