package com.example.bidmart.common.event;

import java.math.BigDecimal;
import java.util.UUID;

public record BalanceHeldEvent(UUID userId, BigDecimal amount, UUID listingId) {}