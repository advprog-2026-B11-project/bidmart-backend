package com.example.bidmart.bidding.service;

import com.example.bidmart.listing.model.AuctionStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ListingSnapshotTest {

    @Test
    void isOpenAt_activeAndEndTimeInFuture_returnsTrue() {
        ListingSnapshot snapshot = new ListingSnapshot(
                UUID.randomUUID(), UUID.randomUUID(),
                BigDecimal.valueOf(100), LocalDateTime.now().plusDays(1),
                AuctionStatus.ACTIVE, null, null
        );
        assertTrue(snapshot.isOpenAt(LocalDateTime.now()));
    }

    @Test
    void isOpenAt_activeAndEndTimeInPast_returnsFalse() {
        ListingSnapshot snapshot = new ListingSnapshot(
                UUID.randomUUID(), UUID.randomUUID(),
                BigDecimal.valueOf(100), LocalDateTime.now().minusDays(1),
                AuctionStatus.ACTIVE, null, null
        );
        assertFalse(snapshot.isOpenAt(LocalDateTime.now()));
    }

    @Test
    void isOpenAt_closedStatus_returnsFalse() {
        ListingSnapshot snapshot = new ListingSnapshot(
                UUID.randomUUID(), UUID.randomUUID(),
                BigDecimal.valueOf(100), LocalDateTime.now().plusDays(1),
                AuctionStatus.CLOSED, null, null
        );
        assertFalse(snapshot.isOpenAt(LocalDateTime.now()));
    }

    @Test
    void isOpenAt_extendedAndEndTimeInFuture_returnsTrue() {
        ListingSnapshot snapshot = new ListingSnapshot(
                UUID.randomUUID(), UUID.randomUUID(),
                BigDecimal.valueOf(100), LocalDateTime.now().plusDays(1),
                AuctionStatus.EXTENDED, null, null
        );
        assertTrue(snapshot.isOpenAt(LocalDateTime.now()));
    }

    @Test
    void isOpenAt_activeAndNullEndTime_returnsTrue() {
        ListingSnapshot snapshot = new ListingSnapshot(
                UUID.randomUUID(), UUID.randomUUID(),
                BigDecimal.valueOf(100), null,
                AuctionStatus.ACTIVE, null, null
        );
        assertTrue(snapshot.isOpenAt(LocalDateTime.now()));
    }

    @Test
    void isOpenAt_closedAndNullEndTime_returnsFalse() {
        ListingSnapshot snapshot = new ListingSnapshot(
                UUID.randomUUID(), UUID.randomUUID(),
                BigDecimal.valueOf(100), null,
                AuctionStatus.CLOSED, null, null
        );
        assertFalse(snapshot.isOpenAt(LocalDateTime.now()));
    }

    @Test
    void isOpenAt_wonStatus_returnsFalse() {
        ListingSnapshot snapshot = new ListingSnapshot(
                UUID.randomUUID(), UUID.randomUUID(),
                BigDecimal.valueOf(100), LocalDateTime.now().plusDays(1),
                AuctionStatus.WON, null, null
        );
        assertFalse(snapshot.isOpenAt(LocalDateTime.now()));
    }

    @Test
    void isOpenAt_unsoldStatus_returnsFalse() {
        ListingSnapshot snapshot = new ListingSnapshot(
                UUID.randomUUID(), UUID.randomUUID(),
                BigDecimal.valueOf(100), LocalDateTime.now().plusDays(1),
                AuctionStatus.UNSOLD, null, null
        );
        assertFalse(snapshot.isOpenAt(LocalDateTime.now()));
    }

    @Test
    void isOpenAt_draftStatus_returnsFalse() {
        ListingSnapshot snapshot = new ListingSnapshot(
                UUID.randomUUID(), UUID.randomUUID(),
                BigDecimal.valueOf(100), LocalDateTime.now().plusDays(1),
                AuctionStatus.DRAFT, null, null
        );
        assertFalse(snapshot.isOpenAt(LocalDateTime.now()));
    }

    @Test
    void recordAccessors_returnCorrectValues() {
        UUID id = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();
        UUID bidderId = UUID.randomUUID();
        BigDecimal price = BigDecimal.valueOf(500);
        BigDecimal highestBid = BigDecimal.valueOf(1000);
        LocalDateTime endTime = LocalDateTime.of(2025, 12, 31, 23, 59);

        ListingSnapshot snapshot = new ListingSnapshot(
                id, sellerId, price, endTime,
                AuctionStatus.ACTIVE, highestBid, bidderId
        );

        assertEquals(id, snapshot.id());
        assertEquals(sellerId, snapshot.sellerId());
        assertEquals(price, snapshot.startingPrice());
        assertEquals(endTime, snapshot.endTime());
        assertEquals(AuctionStatus.ACTIVE, snapshot.auctionStatus());
        assertEquals(highestBid, snapshot.currentHighestBid());
        assertEquals(bidderId, snapshot.currentHighestBidderId());
    }
}
