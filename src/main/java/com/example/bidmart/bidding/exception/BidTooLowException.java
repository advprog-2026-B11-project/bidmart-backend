package com.example.bidmart.bidding.exception;

import java.math.BigDecimal;

public class BidTooLowException extends RuntimeException {

    private final BigDecimal minimumBid;

    public BidTooLowException(String message, BigDecimal minimumBid) {
        super(message);
        this.minimumBid = minimumBid;
    }

    public BigDecimal getMinimumBid() {
        return minimumBid;
    }
}
