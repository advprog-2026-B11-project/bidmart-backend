package com.example.bidmart.bidding.dto;

import com.example.bidmart.bidding.model.Bid;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record BidResponse(
        UUID id,
        UUID listingId,
        UUID buyerId,
        BigDecimal amount,
        Boolean proxyBid,
        BigDecimal proxyMaxLimit,
        LocalDateTime createdAt
) {

    public static BidResponse from(Bid bid) {
        return new BidResponse(
                bid.getId(),
                bid.getListingId(),
                bid.getBuyerId(),
                bid.getAmount(),
                bid.getProxyBid(),
                bid.getProxyMaxLimit(),
                bid.getCreatedAt()
        );
    }
}
