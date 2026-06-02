package com.example.bidmart.user.controller;

import com.example.bidmart.user.dto.SessionResponse;
import com.example.bidmart.user.service.SessionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/sessions")
public class SessionController {
    private final SessionService sessionService;

    public SessionController(SessionService sessionService){
        this.sessionService = sessionService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority(T(com.example.bidmart.common.security.PermissionNames).SESSION_READ)")
    public ResponseEntity<List<SessionResponse>> getMySessions(Authentication authentication){
        return ResponseEntity.ok(sessionService.getActiveSessions(authentication.getName()));
    }

    @DeleteMapping("/{sessionId}")
    @PreAuthorize("hasAuthority(T(com.example.bidmart.common.security.PermissionNames).SESSION_REVOKE)")
    public ResponseEntity<Void> revokeSession(@PathVariable UUID sessionId, Authentication authentication){
        sessionService.revokeSession(authentication.getName(), sessionId);
        return ResponseEntity.noContent().build();
    }
}
