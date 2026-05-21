package com.example.bidmart.listing.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ListingModelTest {

    @Test
    void isReservePriceMet_shouldReturnTrue() {
        Listing listing = new Listing();
        listing.setReservePrice(new BigDecimal("500"));
        listing.setCurrentHighestBid(new BigDecimal("700"));

        assertTrue(listing.isReservePriceMet());
    }

    @Test
    void isReservePriceMet_shouldReturnFalse_whenBidBelowReserve() {
        Listing listing = new Listing();
        listing.setReservePrice(new BigDecimal("500"));
        listing.setCurrentHighestBid(new BigDecimal("300"));

        assertFalse(listing.isReservePriceMet());
    }

    @Test
    void isReservePriceMet_shouldReturnFalse_whenHighestBidNull() {
        Listing listing = new Listing();
        listing.setReservePrice(new BigDecimal("500"));
        listing.setCurrentHighestBid(null);

        assertFalse(listing.isReservePriceMet());
    }

    @Test
    void updateHighestBid_shouldUpdateFields() {
        Listing listing = new Listing();
        UUID bidderId = UUID.randomUUID();
        listing.updateHighestBid(bidderId, new BigDecimal("900"));

        assertEquals(new BigDecimal("900"), listing.getCurrentHighestBid());
        assertEquals(bidderId, listing.getCurrentHighestBidderId());
    }

    @Test
    void isWithinAntiSnipingWindow_shouldReturnTrue() {
        Listing listing = new Listing();
        listing.setEndTime(LocalDateTime.now().plusMinutes(1));

        assertTrue(listing.isWithinAntiSnipingWindow());
    }

    @Test
    void isWithinAntiSnipingWindow_shouldReturnFalse() {
        Listing listing = new Listing();
        listing.setEndTime(LocalDateTime.now().plusHours(1));

        assertFalse(listing.isWithinAntiSnipingWindow());
    }

    @Test
    void extendAuction_shouldExtendEndTime() {
        Listing listing = new Listing();
        LocalDateTime oldTime = LocalDateTime.now().plusMinutes(1);
        listing.setEndTime(oldTime);
        listing.extendAuction();

        assertTrue(listing.getEndTime().isAfter(oldTime));
        assertEquals(AuctionStatus.EXTENDED, listing.getStatus());
    }

    @Test
    void onCreate_shouldSetCreatedAt() {
        Listing listing = new Listing();
        listing.onCreate();

        assertNotNull(listing.getCreatedAt());
    }

    @Test
    void isReservePriceMet_shouldReturnTrue_whenReservePriceNull() {
        Listing listing = new Listing();
        listing.setReservePrice(null);

        assertTrue(listing.isReservePriceMet());
    }

    @Test
    void extendAuction_shouldSetExtendedStatus() {
        Listing listing = new Listing();
        listing.extendAuction();

        assertEquals(AuctionStatus.EXTENDED, listing.getStatus());
    }

    @Test
    void onCreate_shouldSetDefaultStatus() {
        Listing listing = new Listing();
        listing.setStatus(null);
        listing.onCreate();

        assertEquals(AuctionStatus.ACTIVE, listing.getStatus());
    }

    @Test
    void onCreate_shouldNotOverrideExistingStatus() {
        Listing listing = new Listing();
        listing.setStatus(AuctionStatus.CLOSED);
        listing.onCreate();

        assertEquals(AuctionStatus.CLOSED, listing.getStatus());
    }

    @Test
    void onCreate_shouldNotOverrideExistingCreatedAt() {
        Listing listing = new Listing();
        LocalDateTime existing = LocalDateTime.now().minusDays(1);
        listing.setCreatedAt(existing);
        listing.onCreate();

        assertEquals(existing, listing.getCreatedAt());
    }

    @Test
    void allArgsConstructor_shouldSetFields() {
        UUID id = UUID.randomUUID(), sellerId = UUID.randomUUID(),
                categoryId = UUID.randomUUID(), bidderId = UUID.randomUUID();

        LocalDateTime endTime = LocalDateTime.now(), createdAt = LocalDateTime.now();

        Listing listing = new Listing(
                id, sellerId, categoryId, "Laptop", "Gaming", "img.jpg",
                new BigDecimal("100"), new BigDecimal("200"), endTime,
                AuctionStatus.ACTIVE, AuctionType.ENGLISH,
                new BigDecimal("150"), bidderId, 1L, createdAt
        );

        assertEquals(id, listing.getId());
        assertEquals(sellerId, listing.getSellerId());
        assertEquals(categoryId, listing.getCategoryId());
        assertEquals("Laptop", listing.getTitle());
        assertEquals("Gaming", listing.getDescription());
        assertEquals("img.jpg", listing.getImageUrl());
        assertEquals(new BigDecimal("100"), listing.getStartingPrice());
        assertEquals(new BigDecimal("200"), listing.getReservePrice());
        assertEquals(endTime, listing.getEndTime());
        assertEquals(AuctionStatus.ACTIVE, listing.getStatus());
        assertEquals(AuctionType.ENGLISH, listing.getAuctionType());
        assertEquals(new BigDecimal("150"), listing.getCurrentHighestBid());
        assertEquals(bidderId, listing.getCurrentHighestBidderId());
        assertEquals(1L, listing.getVersion());
        assertEquals(createdAt, listing.getCreatedAt());
    }
}