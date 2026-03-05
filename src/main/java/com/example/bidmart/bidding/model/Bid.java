package com.example.bidmart.bidding.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;

import java.util.UUID;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "bids")

public class Bid {

    @Id
    @GeneratedValue
    private UUID id;

    private UUID listingId;
    private UUID buyerId;
    private BigDecimal amount;
    private Boolean isProxyBid;
    private BigDecimal proxyMaxLimit;
    private LocalDateTime createdAt;
}


