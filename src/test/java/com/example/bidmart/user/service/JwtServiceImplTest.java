package com.example.bidmart.user.service;

import com.example.bidmart.user.model.Role;
import com.example.bidmart.user.model.User;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceImplTest {

    private JwtServiceImpl jwtService;
    private User mockUser;

    // A valid 256-bit (32 byte) base64 encoded string for testing
    private final String SECRET_KEY = "VGhpcy1Jcy1BLVZlcnktU2VjdXJlLVRlc3QtU2VjcmV0LUtleS0yNTZiaXRz"; 

    @BeforeEach
    void setUp() {
        jwtService = new JwtServiceImpl();
        ReflectionTestUtils.setField(jwtService, "secretKey", SECRET_KEY);
        ReflectionTestUtils.setField(jwtService, "accessTokenExpiration", 1000L * 60 * 15); // 15 mins
        ReflectionTestUtils.setField(jwtService, "refreshTokenExpiration", 1000L * 60 * 60 * 24); // 1 day

        Role role = new Role();
        role.setName("USER");

        mockUser = new User();
        mockUser.setId(UUID.randomUUID());
        mockUser.setUsername("testuser");
        mockUser.setRole(role);
    }

    @Test
    void generateAccessToken_success() {
        String token = jwtService.generateAccessToken(mockUser);
        assertNotNull(token);

        String username = jwtService.extractUsername(token);
        assertEquals("testuser", username);
    }

    @Test
    void generateRefreshToken_success() {
        String token = jwtService.generateRefreshToken(mockUser);
        assertNotNull(token);

        String username = jwtService.extractUsername(token);
        assertEquals("testuser", username);
    }

    @Test
    void generateTempToken_success() {
        String token = jwtService.generateTempToken(mockUser);
        assertNotNull(token);

        String username = jwtService.extractUsername(token);
        assertEquals("testuser", username);
    }

    @Test
    void isTokenValid_validToken_returnsTrue() {
        String token = jwtService.generateAccessToken(mockUser);
        assertTrue(jwtService.isTokenValid(token, mockUser.getUsername()));
    }

    @Test
    void isTokenValid_invalidUsername_returnsFalse() {
        String token = jwtService.generateAccessToken(mockUser);
        assertFalse(jwtService.isTokenValid(token, "otheruser"));
    }

    @Test
    void extractUsername_expiredToken_throwsException() {
        // Set expiration to a negative value to immediately expire it
        ReflectionTestUtils.setField(jwtService, "accessTokenExpiration", -1000L);
        String token = jwtService.generateAccessToken(mockUser);

        assertThrows(ExpiredJwtException.class, () -> jwtService.extractUsername(token));
    }

    @Test
    void extractUsername_tamperedToken_throwsException() {
        String token = jwtService.generateAccessToken(mockUser);
        String tamperedToken = token + "bad";

        assertThrows(SignatureException.class, () -> jwtService.extractUsername(tamperedToken));
    }
}
