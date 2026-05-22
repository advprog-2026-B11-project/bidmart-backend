package com.example.bidmart.user.service;

import com.example.bidmart.user.dto.SessionResponse;
import com.example.bidmart.user.model.Session;
import com.example.bidmart.user.model.User;
import com.example.bidmart.user.repository.SessionRepository;
import com.example.bidmart.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SessionServiceImplTest {

    @Mock
    private SessionRepository sessionRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SessionServiceImpl sessionService;

    private User user;
    private Session session;
    private final UUID userId = UUID.randomUUID();
    private final UUID sessionId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(userId);
        user.setUsername("testuser");

        session = Session.builder()
                .id(sessionId)
                .user(user)
                .refreshToken("token")
                .isRevoked(false)
                .deviceInfo("device")
                .build();
    }

    @Test
    void enforceSessionLimit_exceedsLimit_revokesOldest() {
        Session session1 = new Session();
        Session session2 = new Session();
        Session session3 = new Session();

        when(sessionRepository.findByUserIdAndIsRevokedFalseOrderByCreatedAtAsc(userId))
                .thenReturn(List.of(session1, session2, session3));

        sessionService.enforceSessionLimit(user, 2);

        assertTrue(session1.isRevoked());
        assertTrue(session2.isRevoked());
        assertFalse(session3.isRevoked());

        verify(sessionRepository).saveAll(List.of(session1, session2));
    }

    @Test
    void enforceSessionLimit_withinLimit_doesNothing() {
        when(sessionRepository.findByUserIdAndIsRevokedFalseOrderByCreatedAtAsc(userId))
                .thenReturn(List.of(session));

        sessionService.enforceSessionLimit(user, 2);

        assertFalse(session.isRevoked());
        verify(sessionRepository, never()).saveAll(any());
    }

    @Test
    void createSession_createsAndReturnsSession() {
        when(sessionRepository.save(any(Session.class))).thenReturn(session);

        SessionResponse response = sessionService.createSession(user, "newToken", "newDevice");

        assertNotNull(response);
        assertEquals("newDevice", response.getDeviceInfo());
        verify(sessionRepository).save(any(Session.class));
    }

    @Test
    void getActiveSessions_returnsSessions() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(sessionRepository.findByUserIdAndIsRevokedFalseOrderByCreatedAtAsc(userId))
                .thenReturn(List.of(session));

        List<SessionResponse> activeSessions = sessionService.getActiveSessions("testuser");

        assertEquals(1, activeSessions.size());
        assertEquals("device", activeSessions.get(0).getDeviceInfo());
    }

    @Test
    void revokeSession_revokesSession() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(sessionRepository.findByIdAndUserId(sessionId, userId)).thenReturn(Optional.of(session));

        sessionService.revokeSession("testuser", sessionId);

        assertTrue(session.isRevoked());
        verify(sessionRepository).save(session);
    }

    @Test
    void revokeAllSessions_revokesAll() {
        Session session1 = new Session();
        Session session2 = new Session();
        when(sessionRepository.findByUserIdAndIsRevokedFalseOrderByCreatedAtAsc(userId))
                .thenReturn(List.of(session1, session2));

        sessionService.revokeAllSessions(user);

        assertTrue(session1.isRevoked());
        assertTrue(session2.isRevoked());
        verify(sessionRepository).saveAll(List.of(session1, session2));
    }

    @Test
    void revokeAllSessions_noActiveSessions_doesNothing() {
        when(sessionRepository.findByUserIdAndIsRevokedFalseOrderByCreatedAtAsc(userId))
                .thenReturn(List.of());

        sessionService.revokeAllSessions(user);

        verify(sessionRepository, never()).saveAll(any());
    }
}
