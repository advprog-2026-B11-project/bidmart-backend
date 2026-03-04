package com.example.bidmart.listing.model;

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
@Table(name = "listings")
public class Listing {

    @Id
    @GeneratedValue
    private UUID id;
    private UUID sellerId;
    private UUID categoryId;
    private String title;
    private String description;
    private BigDecimal startingPrice;
    private BigDecimal reservePrice;
    private LocalDateTime endTime;
    private String status;
    private LocalDateTime createdAt;
}