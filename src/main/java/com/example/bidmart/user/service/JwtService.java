package com.example.bidmart.user.service;

import com.example.bidmart.user.model.User;

/**
 * Abstraction for JWT token operations.
 * Follows Interface Segregation: only JWT-related methods.
 */
public interface JwtService {
    String generateAccessToken(User user);
    String generateRefreshToken(User user);
    String extractUsername(String token);
    boolean isTokenValid(String token, String username);
}
