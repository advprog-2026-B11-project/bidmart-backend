package com.example.bidmart.bidding.dto;

import java.math.BigDecimal;


// Response body when a challenger bid is beaten by an existing proxy (HTTP 422, status -4).

public record ProxyOutbidResponse(
        int status,
        String error,
        String message,
        String timestamp,
        BigDecimal currentHighestBid
) {}
