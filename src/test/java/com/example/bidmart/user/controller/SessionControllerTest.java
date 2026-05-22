package com.example.bidmart.user.controller;

import com.example.bidmart.user.dto.SessionResponse;
import com.example.bidmart.user.service.SessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SessionControllerTest {

    @Mock
    private SessionService sessionService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private SessionController sessionController;

    private UUID sessionId;

    @BeforeEach
    void setUp() {
        sessionId = UUID.randomUUID();
    }

    @Test
    void getMySessions_success() {
        when(authentication.getName()).thenReturn("testuser");
        List<SessionResponse> mockResponses = List.of(new SessionResponse(sessionId, "Chrome", Instant.now(), Instant.now().plusSeconds(3600)));
        when(sessionService.getActiveSessions("testuser")).thenReturn(mockResponses);

        ResponseEntity<List<SessionResponse>> response = sessionController.getMySessions(authentication);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().size());
        assertEquals("Chrome", response.getBody().get(0).getDeviceInfo());
    }

    @Test
    void revokeSession_success() {
        when(authentication.getName()).thenReturn("testuser");

        ResponseEntity<Void> response = sessionController.revokeSession(sessionId, authentication);

        assertEquals(204, response.getStatusCode().value());
        verify(sessionService).revokeSession("testuser", sessionId);
    }
}
