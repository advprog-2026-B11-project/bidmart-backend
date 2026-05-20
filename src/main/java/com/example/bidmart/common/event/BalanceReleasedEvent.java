package com.example.bidmart.common.event;

import java.math.BigDecimal;
import java.util.UUID;

public record BalanceReleasedEvent(UUID userId, BigDecimal amount, UUID listingId) {}