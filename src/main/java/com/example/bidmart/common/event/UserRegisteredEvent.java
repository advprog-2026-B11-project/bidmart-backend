package com.example.bidmart.common.event;

import java.time.Instant;
import java.util.UUID;

public record UserRegisteredEvent(
        UUID userId,
        String username,
        Instant occurredAt
) {}
