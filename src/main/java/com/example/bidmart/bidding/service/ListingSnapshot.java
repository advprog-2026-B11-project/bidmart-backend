package com.example.bidmart.bidding.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

public record ListingSnapshot(
        UUID id,
        UUID sellerId,
        BigDecimal startingPrice,
        LocalDateTime endTime,
        String status
) {

    public boolean isOpenAt(LocalDateTime currentTime) {
        if (endTime != null && !endTime.isAfter(currentTime)) {
            return false;
        }

        if (status == null || status.isBlank()) {
            return true;
        }

        String normalized = status.trim().toUpperCase(Locale.ROOT);
        return "OPEN".equals(normalized) || "ACTIVE".equals(normalized);
    }
}
