package com.example.bidmart.bidding.dto;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public record MockWalletStateResponse(
        UUID buyerId,
        BigDecimal availableBalance,
        Map<UUID, BigDecimal> lockedByListing
) {
}
