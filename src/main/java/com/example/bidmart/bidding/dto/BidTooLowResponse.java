package com.example.bidmart.bidding.dto;

import java.math.BigDecimal;

public record BidTooLowResponse(
        int status,
        String error,
        String message,
        String timestamp,
        BigDecimal minimumBid
) {}
