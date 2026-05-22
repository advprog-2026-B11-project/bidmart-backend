package com.example.bidmart.user.service;

import com.example.bidmart.user.dto.UpdateProfileRequest;
import com.example.bidmart.user.dto.UserProfileResponse;
import com.example.bidmart.user.model.User;
import com.example.bidmart.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplCoverageTest {

    @Mock private UserRepository userRepository;
    @InjectMocks private UserServiceImpl userService;

    @Test
    void updateProfile_success() {
        User user = new User();
        user.setUsername("alice");
        user.setDisplayName("Alice");
        
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setDisplayName("Alice Updated");

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        UserProfileResponse response = userService.updateProfile("alice", request);

        assertEquals("Alice Updated", response.getDisplayName());
    }
}