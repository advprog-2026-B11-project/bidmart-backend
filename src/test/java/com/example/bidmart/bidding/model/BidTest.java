package com.example.bidmart.bidding.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class BidTest {

    @Test
    void getReservedAmount_proxyBidWithMaxLimit_returnsProxyMaxLimit() {
        Bid bid = new Bid();
        bid.setAmount(BigDecimal.valueOf(100));
        bid.setProxyBid(true);
        bid.setProxyMaxLimit(BigDecimal.valueOf(500));

        assertEquals(BigDecimal.valueOf(500), bid.getReservedAmount());
    }

    @Test
    void getReservedAmount_proxyBidWithoutMaxLimit_returnsAmount() {
        Bid bid = new Bid();
        bid.setAmount(BigDecimal.valueOf(100));
        bid.setProxyBid(true);
        bid.setProxyMaxLimit(null);

        assertEquals(BigDecimal.valueOf(100), bid.getReservedAmount());
    }

    @Test
    void getReservedAmount_notProxyBid_returnsAmount() {
        Bid bid = new Bid();
        bid.setAmount(BigDecimal.valueOf(100));
        bid.setProxyBid(false);

        assertEquals(BigDecimal.valueOf(100), bid.getReservedAmount());
    }

    @Test
    void getReservedAmount_nullProxyBid_returnsAmount() {
        Bid bid = new Bid();
        bid.setAmount(BigDecimal.valueOf(100));
        bid.setProxyBid(null);

        assertEquals(BigDecimal.valueOf(100), bid.getReservedAmount());
    }

    @Test
    void getReservedAmount_proxyBidFalseWithMaxLimit_returnsAmount() {
        Bid bid = new Bid();
        bid.setAmount(BigDecimal.valueOf(100));
        bid.setProxyBid(false);
        bid.setProxyMaxLimit(BigDecimal.valueOf(500));

        assertEquals(BigDecimal.valueOf(100), bid.getReservedAmount());
    }

    @Test
    void onCreate_setsCreatedAtWhenNull() {
        Bid bid = new Bid();
        assertNull(bid.getCreatedAt());
        // proxyBid defaults to FALSE via field initializer
        assertEquals(Boolean.FALSE, bid.getProxyBid());

        bid.onCreate();

        assertNotNull(bid.getCreatedAt());
        assertEquals(Boolean.FALSE, bid.getProxyBid());
    }

    @Test
    void onCreate_setsProxyBidWhenNull() {
        Bid bid = new Bid();
        // Explicitly set proxyBid to null to test the null branch
        bid.setProxyBid(null);

        bid.onCreate();

        assertNotNull(bid.getCreatedAt());
        assertEquals(Boolean.FALSE, bid.getProxyBid());
    }

    @Test
    void onCreate_doesNotOverwriteExistingProxyBid() {
        Bid bid = new Bid();
        bid.setProxyBid(true);

        bid.onCreate();

        assertEquals(true, bid.getProxyBid());
    }

    @Test
    void allArgsConstructor_setsAllFields() {
        UUID id = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();
        UUID buyerId = UUID.randomUUID();
        BigDecimal amount = BigDecimal.valueOf(200);
        LocalDateTime createdAt = LocalDateTime.now();

        Bid bid = new Bid(id, listingId, buyerId, amount, true, BigDecimal.valueOf(500), createdAt);

        assertEquals(id, bid.getId());
        assertEquals(listingId, bid.getListingId());
        assertEquals(buyerId, bid.getBuyerId());
        assertEquals(amount, bid.getAmount());
        assertEquals(true, bid.getProxyBid());
        assertEquals(BigDecimal.valueOf(500), bid.getProxyMaxLimit());
        assertEquals(createdAt, bid.getCreatedAt());
    }
}
