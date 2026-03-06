package com.example.bidmart.listing.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Column;

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
@Table(name = "listings")
public class Listing {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "seller_id", nullable = false)
    private UUID sellerId;

    @Column(name = "category_id", nullable = false)
    private UUID categoryId;

    @Column(nullable = false)
    private String title;

    private String description;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "starting_price", nullable = false)
    private BigDecimal startingPrice;

    @Column(name = "reserve_price")
    private BigDecimal reservePrice;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    private String status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}