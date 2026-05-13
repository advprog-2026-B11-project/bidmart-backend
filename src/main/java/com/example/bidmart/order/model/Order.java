package com.example.bidmart.order.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "listing_id", nullable = false)
    private UUID listingId;

    @Column(name = "buyer_id", nullable = false)
    private UUID buyerId;

    @Column(name = "seller_id", nullable = false)
    private UUID sellerId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(name = "dispute_reason")
    private String disputeReason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(name = "tracking_number")
    private String trackingNumber;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Order(UUID listingId, UUID buyerId, UUID sellerId, BigDecimal amount, OrderStatus status) {
        this.listingId = listingId;
        this.buyerId = buyerId;
        this.sellerId = sellerId;
        this.amount = amount;
        this.status = status;
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.status == null) {
            this.status = OrderStatus.CREATED;
        }
    }
}