package com.example.bidmart.common.event;

import java.math.BigDecimal;
import java.util.UUID;

public record BalanceSettledEvent(UUID userId, BigDecimal amount, String referenceId) {}
