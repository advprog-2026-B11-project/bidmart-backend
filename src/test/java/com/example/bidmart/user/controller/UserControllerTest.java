package com.example.bidmart.user.controller;

import com.example.bidmart.user.dto.MfaDisableRequest;
import com.example.bidmart.user.dto.MfaEmailVerifyRequest;
import com.example.bidmart.user.dto.MfaEnableRequest;
import com.example.bidmart.user.dto.MfaSetupResponse;
import com.example.bidmart.user.dto.MfaStatusResponse;
import com.example.bidmart.user.dto.UpdateProfileRequest;
import com.example.bidmart.user.dto.UserProfileResponse;
import com.example.bidmart.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private UserController userController;

    private UserProfileResponse profileResponse;

    @BeforeEach
    void setUp() {
        profileResponse = UserProfileResponse.builder()
                .id(UUID.randomUUID())
                .username("alice")
                .email("alice@mail.com")
                .displayName("Alice")
                .phoneNumber("08123456789")
                .role("USER")
                .isEmailVerified(false)
                .build();

        when(authentication.getName()).thenReturn("alice");
    }

    @Test
    void updateProfile_shouldReturnUpdatedProfile() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setDisplayName("Alice Updated");

        UserProfileResponse updated = UserProfileResponse.builder()
                .id(profileResponse.getId())
                .username("alice")
                .email("alice@mail.com")
                .displayName("Alice Updated")
                .phoneNumber("08123456789")
                .role("USER")
                .isEmailVerified(false)
                .build();

        when(userService.updateProfile("alice", request)).thenReturn(updated);

        ResponseEntity<UserProfileResponse> response = userController.updateProfile(authentication, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Alice Updated", response.getBody().getDisplayName());
        verify(userService, times(1)).updateProfile("alice", request);
    }

    @Test
    void getCurrentUser_shouldReturnProfile() {
        when(userService.getCurrentUser("alice")).thenReturn(profileResponse);

        ResponseEntity<UserProfileResponse> response = userController.getCurrentUser(authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("alice", response.getBody().getUsername());
        verify(userService, times(1)).getCurrentUser("alice");
    }

    @Test
    void deleteProfile_shouldReturnNoContent() {
        ResponseEntity<Void> response = userController.deleteProfile(authentication);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(userService, times(1)).deleteProfile("alice");
    }

    @Test
    void getMfaStatus_shouldReturnStatus() {
        MfaStatusResponse statusResponse = new MfaStatusResponse(true, "TOTP");
        when(userService.getMfaStatus("alice")).thenReturn(statusResponse);

        ResponseEntity<MfaStatusResponse> response = userController.getMfaStatus(authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isEnabled());
        assertEquals("TOTP", response.getBody().getMethod());
        verify(userService, times(1)).getMfaStatus("alice");
    }

    @Test
    void setupMfa_shouldReturnSetupResponse() {
        MfaSetupResponse setupResponse = new MfaSetupResponse("SECRET", "QR_URI", "TOTP", false);
        when(userService.setupMfa("alice")).thenReturn(setupResponse);

        ResponseEntity<MfaSetupResponse> response = userController.setupMfa(authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("SECRET", response.getBody().getSecret());
        verify(userService, times(1)).setupMfa("alice");
    }

    @Test
    void enableMfa_shouldReturnStatus() {
        MfaEnableRequest req = new MfaEnableRequest();
        req.setCode("123456");
        MfaStatusResponse statusResponse = new MfaStatusResponse(true, "TOTP");
        when(userService.enableMfa("alice", "123456")).thenReturn(statusResponse);

        ResponseEntity<MfaStatusResponse> response = userController.enableMfa(authentication, req);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isEnabled());
        verify(userService, times(1)).enableMfa("alice", "123456");
    }

    @Test
    void enableEmailMfa_shouldReturnStatus() {
        MfaStatusResponse statusResponse = new MfaStatusResponse(false, "EMAIL");
        when(userService.enableEmailMfa("alice")).thenReturn(statusResponse);

        ResponseEntity<MfaStatusResponse> response = userController.enableEmailMfa(authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("EMAIL", response.getBody().getMethod());
        verify(userService, times(1)).enableEmailMfa("alice");
    }

    @Test
    void verifyEmailMfa_shouldReturnStatus() {
        MfaEmailVerifyRequest request = new MfaEmailVerifyRequest();
        request.setCode("654321");
        MfaStatusResponse statusResponse = new MfaStatusResponse(true, "EMAIL");
        when(userService.verifyEmailMfa("alice", "654321")).thenReturn(statusResponse);

        ResponseEntity<MfaStatusResponse> response = userController.verifyEmailMfa(authentication, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isEnabled());
        assertEquals("EMAIL", response.getBody().getMethod());
        verify(userService, times(1)).verifyEmailMfa("alice", "654321");
    }

    @Test
    void disableMfa_shouldReturnStatus() {
        MfaDisableRequest req = new MfaDisableRequest();
        req.setPassword("password");
        req.setTotpCode("123456");
        MfaStatusResponse statusResponse = new MfaStatusResponse(false, "NONE");
        when(userService.disableMfa("alice", "password", "123456")).thenReturn(statusResponse);

        ResponseEntity<MfaStatusResponse> response = userController.disableMfa(authentication, req);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertFalse(response.getBody().isEnabled());
        verify(userService, times(1)).disableMfa("alice", "password", "123456");
    }
}
