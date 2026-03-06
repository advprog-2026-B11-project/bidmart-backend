package com.example.bidmart.bidding.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record MockListingUpsertRequest(
        UUID id,
        UUID sellerId,
        BigDecimal startingPrice,
        LocalDateTime endTime,
        String status
) {
}
