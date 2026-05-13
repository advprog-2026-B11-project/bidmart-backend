package com.example.bidmart.common.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class OrderRefundedEvent {
    private final UUID orderId;
    private final UUID buyerId;
    private final BigDecimal amount;
}