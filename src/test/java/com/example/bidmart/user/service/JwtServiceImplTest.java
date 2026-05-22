package com.example.bidmart.user.service;

import com.example.bidmart.user.model.Role;
import com.example.bidmart.user.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceImplTest {

    @Test
    void generateAccessToken_shouldIncludeSessionId() {
        JwtServiceImpl jwtService = new JwtServiceImpl();
        ReflectionTestUtils.setField(jwtService, "secretKey", "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=");
        ReflectionTestUtils.setField(jwtService, "accessTokenExpiration", 60000L);
        ReflectionTestUtils.setField(jwtService, "refreshTokenExpiration", 120000L);

        Role role = new Role();
        role.setName("USER");
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("alice");
        user.setRole(role);

        UUID sessionId = UUID.randomUUID();
        String token = jwtService.generateAccessToken(user, sessionId);

        assertNotNull(token);
        assertEquals("alice", jwtService.extractUsername(token));
        assertEquals(sessionId, jwtService.extractSessionId(token));
        assertTrue(jwtService.isTokenValid(token, "alice"));
    }

    @Test
    void generateTempToken_shouldReturnValidToken() {
        JwtServiceImpl jwtService = new JwtServiceImpl();
        ReflectionTestUtils.setField(jwtService, "secretKey", "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=");
        ReflectionTestUtils.setField(jwtService, "accessTokenExpiration", 60000L);
        ReflectionTestUtils.setField(jwtService, "refreshTokenExpiration", 120000L);

        Role role = new Role();
        role.setName("USER");
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("alice");
        user.setRole(role);

        String token = jwtService.generateTempToken(user);

        assertNotNull(token);
        assertEquals("alice", jwtService.extractUsername(token));
        assertTrue(jwtService.isTokenValid(token, "alice"));
    }
}
