package com.example.bidmart.bidding.exception;

import java.math.BigDecimal;


// Thrown when a challenger bid is automatically countered and defeated by an existing active proxy.
// Corresponds to business status code -4.

public class ProxyChallengerOutbidException extends RuntimeException {

    private final BigDecimal currentHighestBid;

    public ProxyChallengerOutbidException(String message, BigDecimal currentHighestBid) {
        super(message);
        this.currentHighestBid = currentHighestBid;
    }

    public BigDecimal getCurrentHighestBid() {
        return currentHighestBid;
    }
}
