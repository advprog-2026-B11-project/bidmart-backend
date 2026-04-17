package com.example.bidmart.bidding.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "bids")
public class Bid {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "listing_id", nullable = false)
    private UUID listingId;

    @Column(name = "buyer_id", nullable = false)
    private UUID buyerId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "is_proxy_bid")
    private Boolean proxyBid = Boolean.FALSE;

    @Column(name = "proxy_max_limit", precision = 19, scale = 2)
    private BigDecimal proxyMaxLimit;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }

        if (proxyBid == null) {
            proxyBid = Boolean.FALSE;
        }
    }

    public BigDecimal getReservedAmount() {
        if (Boolean.TRUE.equals(proxyBid) && proxyMaxLimit != null) {
            return proxyMaxLimit;
        }

        return amount;
    }
}
