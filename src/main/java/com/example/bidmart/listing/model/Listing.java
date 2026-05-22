package com.example.bidmart.listing.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.persistence.Index;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

@Entity
@Table(name = "listings", indexes = {
        @Index(name = "idx_listing_status", columnList = "status"),
        @Index(name = "idx_listing_end_time", columnList = "end_time"),
        @Index(name = "idx_listing_seller_id", columnList = "seller_id"),
        @Index(name = "idx_listing_category_id", columnList = "category_id")
})
public class Listing {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "seller_id", nullable = false)
    private UUID sellerId;

    @Column(name = "category_id", nullable = false)
    private UUID categoryId;

    @NotBlank @Column(nullable = false)
    private String title;

    private String description;

    @Column(name = "image_url")
    private String imageUrl;

    @DecimalMin("0.01") @Column(name = "starting_price", nullable = false)
    private BigDecimal startingPrice;

    @DecimalMin("0.01") @Column(name = "reserve_price")
    private BigDecimal reservePrice;

    @Future @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuctionStatus status = AuctionStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(name = "auction_type", nullable = false)
    private AuctionType auctionType = AuctionType.ENGLISH;

    @Column(name = "current_highest_bid")
    private BigDecimal currentHighestBid;

    @Column(name = "current_highest_bidder_id")
    private UUID currentHighestBidderId;

    @Version
    private Long version;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (status == null) {
            status = AuctionStatus.ACTIVE;
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public void updateHighestBid(UUID bidderId, BigDecimal amount) {
        this.currentHighestBidderId = bidderId;
        this.currentHighestBid = amount;
    }

    public boolean isReservePriceMet() {
        if (reservePrice == null) return true;
        if (currentHighestBid == null) return false;
        return currentHighestBid.compareTo(reservePrice) >= 0;
    }

    public void extendAuction() {
        this.endTime = LocalDateTime.now().plusMinutes(2);
        this.status = AuctionStatus.EXTENDED;
    }

    public boolean isWithinAntiSnipingWindow() {
        return LocalDateTime.now().isAfter(endTime.minusMinutes(2));
    }
}
