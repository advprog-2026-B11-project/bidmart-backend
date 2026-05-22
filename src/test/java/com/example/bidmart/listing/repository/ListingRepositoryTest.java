package com.example.bidmart.listing.repository;

import com.example.bidmart.listing.model.AuctionStatus;
import com.example.bidmart.listing.model.AuctionType;
import com.example.bidmart.listing.model.Listing;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class ListingRepositoryTest {

    @Autowired
    private ListingRepository listingRepository;

    @BeforeEach
    void setUp() {
        listingRepository.deleteAll();
    }

    private Listing listing(String title, String description, BigDecimal startingPrice, AuctionStatus status) {
        Listing listing = new Listing();
        listing.setSellerId(UUID.randomUUID());
        listing.setCategoryId(UUID.randomUUID());
        listing.setTitle(title);
        listing.setDescription(description);
        listing.setStartingPrice(startingPrice);
        listing.setEndTime(LocalDateTime.now().plusDays(1));
        listing.setStatus(status);
        listing.setAuctionType(AuctionType.ENGLISH);
        return listing;
    }
}