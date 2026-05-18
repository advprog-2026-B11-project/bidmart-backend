package com.example.bidmart.listing.dto;

import com.example.bidmart.listing.model.AuctionStatus;
import com.example.bidmart.listing.model.AuctionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ListingResponse {
    private UUID id;
    private UUID sellerId;
    private UUID categoryId;
    private String title;
    private String description;
    private String imageUrl;
    private BigDecimal startingPrice;
    private BigDecimal reservePrice;
    private LocalDateTime endTime;
    private AuctionStatus status;
    private AuctionType auctionType;
    private BigDecimal currentHighestBid;
    private LocalDateTime createdAt;
}