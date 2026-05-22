package com.example.bidmart.bidding.validator;

import com.example.bidmart.bidding.dto.CreateBidRequest;
import com.example.bidmart.bidding.exception.BidTooLowException;
import com.example.bidmart.bidding.exception.BidValidationException;
import com.example.bidmart.bidding.model.Bid;
import com.example.bidmart.bidding.service.ListingSnapshot;
import com.example.bidmart.listing.model.AuctionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class StandardBidValidatorImplTest {

    private StandardBidValidatorImpl validator;
    private UUID buyerId;
    private UUID listingId;
    private ListingSnapshot activeListing;
    private Bid currentHighestBid;

    @BeforeEach
    void setUp() {
        validator = new StandardBidValidatorImpl();
        buyerId = UUID.randomUUID();
        listingId = UUID.randomUUID();
        
        activeListing = new ListingSnapshot(
                listingId,
                UUID.randomUUID(), // sellerId
                BigDecimal.valueOf(100),
                LocalDateTime.now().plusDays(1),
                AuctionStatus.ACTIVE,
                null,
                null
        );

        currentHighestBid = new Bid();
        currentHighestBid.setId(UUID.randomUUID());
        currentHighestBid.setAmount(BigDecimal.valueOf(200));
    }

    @Test
    void validateRequest_validRequest() {
        CreateBidRequest request = new CreateBidRequest(listingId, BigDecimal.valueOf(100), false, null);
        assertDoesNotThrow(() -> validator.validateRequest(request, buyerId));
    }

    @Test
    void validateRequest_nullRequest_throwsException() {
        assertThrows(BidValidationException.class, () -> validator.validateRequest(null, buyerId));
    }

    @Test
    void validateRequest_nullListingId_throwsException() {
        CreateBidRequest request = new CreateBidRequest(null, BigDecimal.valueOf(100), false, null);
        assertThrows(BidValidationException.class, () -> validator.validateRequest(request, buyerId));
    }

    @Test
    void validateRequest_nullBuyerId_throwsException() {
        CreateBidRequest request = new CreateBidRequest(listingId, BigDecimal.valueOf(100), false, null);
        assertThrows(BidValidationException.class, () -> validator.validateRequest(request, null));
    }

    @Test
    void validateRequest_invalidAmount_throwsException() {
        CreateBidRequest request = new CreateBidRequest(listingId, BigDecimal.ZERO, false, null);
        assertThrows(BidValidationException.class, () -> validator.validateRequest(request, buyerId));
    }

    @Test
    void validateRequest_validProxyBid() {
        CreateBidRequest request = new CreateBidRequest(listingId, BigDecimal.valueOf(100), true, BigDecimal.valueOf(500));
        assertDoesNotThrow(() -> validator.validateRequest(request, buyerId));
    }

    @Test
    void validateRequest_proxyBidWithoutMaxLimit_throwsException() {
        CreateBidRequest request = new CreateBidRequest(listingId, BigDecimal.valueOf(100), true, null);
        assertThrows(BidValidationException.class, () -> validator.validateRequest(request, buyerId));
    }

    @Test
    void validateRequest_proxyBidMaxLimitLessThanAmount_throwsException() {
        CreateBidRequest request = new CreateBidRequest(listingId, BigDecimal.valueOf(200), true, BigDecimal.valueOf(100));
        assertThrows(BidValidationException.class, () -> validator.validateRequest(request, buyerId));
    }

    @Test
    void validateBidContext_validContext() {
        assertDoesNotThrow(() -> validator.validateBidContext(buyerId, activeListing, BigDecimal.valueOf(250), Optional.of(currentHighestBid)));
    }

    @Test
    void validateBidContext_buyerIsSeller_throwsException() {
        ListingSnapshot listingWhereBuyerIsSeller = new ListingSnapshot(
                listingId,
                buyerId, // sellerId is buyerId
                BigDecimal.valueOf(100),
                LocalDateTime.now().plusDays(1),
                AuctionStatus.ACTIVE,
                null,
                null
        );
        assertThrows(BidValidationException.class, () -> validator.validateBidContext(buyerId, listingWhereBuyerIsSeller, BigDecimal.valueOf(250), Optional.of(currentHighestBid)));
    }

    @Test
    void validateBidContext_auctionClosed_throwsException() {
        ListingSnapshot closedListing = new ListingSnapshot(
                listingId,
                UUID.randomUUID(),
                BigDecimal.valueOf(100),
                LocalDateTime.now().minusDays(1),
                AuctionStatus.CLOSED,
                null,
                null
        );
        assertThrows(BidValidationException.class, () -> validator.validateBidContext(buyerId, closedListing, BigDecimal.valueOf(250), Optional.of(currentHighestBid)));
    }

    @Test
    void validateBidContext_auctionEndedButStatusActive_throwsException() {
        ListingSnapshot endedListing = new ListingSnapshot(
                listingId,
                UUID.randomUUID(),
                BigDecimal.valueOf(100),
                LocalDateTime.now().minusDays(1),
                AuctionStatus.ACTIVE,
                null,
                null
        );
        assertThrows(BidValidationException.class, () -> validator.validateBidContext(buyerId, endedListing, BigDecimal.valueOf(250), Optional.of(currentHighestBid)));
    }

    @Test
    void validateBidContext_bidTooLowWithHighestBid_throwsException() {
        assertThrows(BidTooLowException.class, () -> validator.validateBidContext(buyerId, activeListing, BigDecimal.valueOf(150), Optional.of(currentHighestBid)));
    }

    @Test
    void validateBidContext_bidTooLowWithoutHighestBid_throwsException() {
        assertThrows(BidTooLowException.class, () -> validator.validateBidContext(buyerId, activeListing, BigDecimal.valueOf(50), Optional.empty()));
    }
}
