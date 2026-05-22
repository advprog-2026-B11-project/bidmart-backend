package com.example.bidmart.common.event;

import java.math.BigDecimal;
import java.util.UUID;

public record WithdrawEvent(UUID userId, BigDecimal amount, String bankName) {}