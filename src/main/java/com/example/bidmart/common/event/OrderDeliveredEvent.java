package com.example.bidmart.common.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class OrderDeliveredEvent {
    private final UUID orderId;
    private final UUID listingId;
    private final UUID buyerId;
    private final UUID sellerId;
    private final BigDecimal amount;
}
