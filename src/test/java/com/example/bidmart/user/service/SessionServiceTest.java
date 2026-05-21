package com.example.bidmart.user.service;

import com.example.bidmart.user.model.Session;
import com.example.bidmart.user.model.SessionOverflowPolicy;
import com.example.bidmart.user.model.User;
import com.example.bidmart.user.repository.SessionRepository;
import com.example.bidmart.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SessionServiceTest {

    @Mock
    private SessionRepository sessionRepository;

    @Mock
    private UserRepository userRepository;

    private SessionServiceImpl sessionService;
    private User user;

    @BeforeEach
    void setUp() {
        sessionService = new SessionServiceImpl(sessionRepository, userRepository, SessionOverflowPolicy.REVOKE_OLDEST);
        user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("alice");
    }

    @Test
    void revokeSession_shouldRevokeAndSave() {
        UUID sessionId = UUID.randomUUID();
        Session session = new Session();
        session.setId(sessionId);
        session.setUser(user);
        session.setRevoked(false);

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(sessionRepository.findByIdAndUserId(sessionId, user.getId())).thenReturn(Optional.of(session));

        sessionService.revokeSession("alice", sessionId);

        assertTrue(session.isRevoked());
        verify(sessionRepository, times(1)).save(session);
    }

    @Test
    void revokeSession_shouldThrowWhenSessionNotFound() {
        UUID sessionId = UUID.randomUUID();

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(sessionRepository.findByIdAndUserId(sessionId, user.getId())).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> sessionService.revokeSession("alice", sessionId));
        verify(sessionRepository, never()).save(any(Session.class));
    }

    @Test
    void enforceSessionLimit_shouldRevokeOldestSessions() {
        Session first = new Session();
        Session second = new Session();
        Session third = new Session();
        first.setRevoked(false);
        second.setRevoked(false);
        third.setRevoked(false);

        List<Session> sessions = new ArrayList<>();
        sessions.add(first);
        sessions.add(second);
        sessions.add(third);

        when(sessionRepository.findByUserIdAndIsRevokedFalseOrderByCreatedAtAsc(user.getId()))
                .thenReturn(sessions);

        sessionService.enforceSessionLimit(user, 2);

        assertTrue(first.isRevoked());
        assertTrue(second.isRevoked());
        assertTrue(!third.isRevoked());
        verify(sessionRepository, times(1)).saveAll(sessions.subList(0, 2));
    }

    @Test
    void getActiveSessions_shouldReturnResponses() {
        Session session = new Session();
        session.setId(UUID.randomUUID());
        session.setDeviceInfo("Device");

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(sessionRepository.findByUserIdAndIsRevokedFalseOrderByCreatedAtAsc(user.getId()))
                .thenReturn(List.of(session));

        assertEquals(1, sessionService.getActiveSessions("alice").size());
        verify(sessionRepository, times(1)).findByUserIdAndIsRevokedFalseOrderByCreatedAtAsc(user.getId());
    }
    
    @Test
    void revokeAllSessions_shouldSetRevokedTrueForAllActiveSessions() {
        Session s1 = new Session(); s1.setRevoked(false);
        Session s2 = new Session(); s2.setRevoked(false);
        
        when(sessionRepository.findByUserIdAndIsRevokedFalseOrderByCreatedAtAsc(user.getId()))
                .thenReturn(List.of(s1, s2));

        sessionService.revokeAllSessions(user);

        assertTrue(s1.isRevoked());
        assertTrue(s2.isRevoked());
        verify(sessionRepository, times(1)).saveAll(any());
    }
}
