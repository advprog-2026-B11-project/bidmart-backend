package com.example.bidmart.user.service;

import com.example.bidmart.user.model.User;
import java.util.UUID;

public interface JwtService {
    @Deprecated
    default String generateAccessToken(User user) {
        throw new UnsupportedOperationException("Use generateAccessToken(user, sessionId)");
    }

    String generateAccessToken(User user, UUID sessionId);
    String generateRefreshToken(User user);
    String extractUsername(String token);
    UUID extractSessionId(String token);
    boolean isTokenValid(String token, String username);
    String generateTempToken(User user);
}
