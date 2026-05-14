package com.example.bidmart.user.controller;

import com.example.bidmart.user.dto.SessionResponse;
import com.example.bidmart.user.service.SessionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SessionControllerTest {

    @Mock
    private SessionService sessionService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private SessionController sessionController;

    @Test
    void getMySessions_shouldReturnOk() {
        UUID sessionId = UUID.randomUUID();
        SessionResponse responseItem = SessionResponse.builder()
                .id(sessionId)
                .deviceInfo("Device")
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        when(authentication.getName()).thenReturn("alice");
        when(sessionService.getActiveSessions("alice")).thenReturn(List.of(responseItem));

        ResponseEntity<List<SessionResponse>> response = sessionController.getMySessions(authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        verify(sessionService, times(1)).getActiveSessions("alice");
    }

    @Test
    void revokeSession_shouldReturnNoContent() {
        UUID sessionId = UUID.randomUUID();
        when(authentication.getName()).thenReturn("alice");

        ResponseEntity<Void> response = sessionController.revokeSession(sessionId, authentication);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(sessionService, times(1)).revokeSession("alice", sessionId);
    }
}
