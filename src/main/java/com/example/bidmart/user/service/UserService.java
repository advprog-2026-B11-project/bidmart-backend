package com.example.bidmart.user.service;

import java.util.UUID;

import com.example.bidmart.user.dto.MfaSetupResponse;
import com.example.bidmart.user.dto.MfaStatusResponse;
import com.example.bidmart.user.dto.UpdateProfileRequest;
import com.example.bidmart.user.dto.UserProfileResponse;
import com.example.bidmart.user.model.Role;

//Abstraction for user profile operations.
public interface UserService {
    UserProfileResponse getCurrentUser(String username);
    UserProfileResponse updateProfile(String username, UpdateProfileRequest request);
    MfaStatusResponse getMfaStatus(String username);
    MfaSetupResponse setupMfa(String username);
    MfaStatusResponse enableMfa(String username, String code);
    MfaStatusResponse enableEmailMfa(String username);
    MfaStatusResponse disableMfa(String username, String password, String totpCode);
    void deactivateUser(UUID userId);
    void changeUserRole(UUID userId, Role newRole);
    void deleteProfile(String username);
    UUID getUserIdByUsername(String username);
}
