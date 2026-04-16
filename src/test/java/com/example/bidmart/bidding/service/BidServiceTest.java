package com.example.bidmart.bidding.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.bidmart.bidding.dto.BidResponse;
import com.example.bidmart.bidding.dto.CreateBidRequest;
import com.example.bidmart.bidding.exception.BidValidationException;
import com.example.bidmart.bidding.exception.InsufficientBalanceException;
import com.example.bidmart.bidding.model.Bid;
import com.example.bidmart.bidding.repository.BidRepository;
import com.example.bidmart.listing.model.Listing;
import com.example.bidmart.listing.service.ListingService;
import com.example.bidmart.wallet.service.WalletService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BidServiceTest {

    @Mock
    private BidRepository bidRepository;

    @Mock
    private ListingService listingService;

    @Mock
    private WalletService walletService;

    private BidService bidService;

    @BeforeEach
    void setUp() {
        bidService = new BidService(bidRepository, listingService, walletService);
    }

    // --- helpers ---

    private Listing activeListing(UUID listingId, UUID sellerId, BigDecimal startingPrice) {
        Listing listing = new Listing();
        listing.setId(listingId);
        listing.setSellerId(sellerId);
        listing.setStartingPrice(startingPrice);
        listing.setEndTime(LocalDateTime.now().plusHours(2));
        listing.setStatus("ACTIVE");
        return listing;
    }

    private Bid savedBid(UUID listingId, UUID buyerId, BigDecimal amount) {
        Bid bid = new Bid();
        bid.setId(UUID.randomUUID());
        bid.setListingId(listingId);
        bid.setBuyerId(buyerId);
        bid.setAmount(amount);
        bid.setProxyBid(Boolean.FALSE);
        bid.setCreatedAt(LocalDateTime.now());
        return bid;
    }

    // --- tests ---

    @Test
    void placeBidSuccess() {
        UUID listingId = UUID.randomUUID();
        UUID buyerId = UUID.randomUUID();
        BigDecimal bidAmount = new BigDecimal("150.00");

        when(listingService.getListingById(listingId))
                .thenReturn(Optional.of(activeListing(listingId, UUID.randomUUID(), new BigDecimal("100.00"))));
        when(bidRepository.findTopByListingIdOrderByAmountDescCreatedAtAsc(listingId))
                .thenReturn(Optional.empty());
        when(bidRepository.findTopByListingIdAndBuyerIdOrderByCreatedAtDesc(listingId, buyerId))
                .thenReturn(Optional.empty());
        when(bidRepository.save(any(Bid.class))).thenAnswer(inv -> savedBid(listingId, buyerId, bidAmount));

        BidResponse response = bidService.placeBid(buyerId, new CreateBidRequest(
                listingId, bidAmount, Boolean.FALSE, null));

        assertEquals(listingId, response.listingId());
        assertEquals(buyerId, response.buyerId());
        assertEquals(bidAmount, response.amount());

        // Full amount should be locked since buyer has no previous reservation.
        verify(walletService).reserveBidFunds(buyerId, listingId, bidAmount);
    }

    @Test
    void placeBidFailedWhenAmountLowerThanCurrentHighest() {
        UUID listingId = UUID.randomUUID();
        UUID buyerId = UUID.randomUUID();

        Bid highestBid = new Bid();
        highestBid.setListingId(listingId);
        highestBid.setBuyerId(UUID.randomUUID());
        highestBid.setAmount(new BigDecimal("200.00"));
        highestBid.setProxyBid(Boolean.FALSE);

        when(listingService.getListingById(listingId))
                .thenReturn(Optional.of(activeListing(listingId, UUID.randomUUID(), new BigDecimal("100.00"))));
        when(bidRepository.findTopByListingIdOrderByAmountDescCreatedAtAsc(listingId))
                .thenReturn(Optional.of(highestBid));

        assertThrows(BidValidationException.class, () -> bidService.placeBid(buyerId, new CreateBidRequest(
                listingId, new BigDecimal("150.00"), Boolean.FALSE, null)));

        verify(bidRepository, never()).save(any(Bid.class));
        verify(walletService, never()).reserveBidFunds(any(), any(), any());
    }

    @Test
    void placeBidFailedWhenBalanceInsufficient() {
        UUID listingId = UUID.randomUUID();
        UUID buyerId = UUID.randomUUID();
        BigDecimal bidAmount = new BigDecimal("120.00");

        when(listingService.getListingById(listingId))
                .thenReturn(Optional.of(activeListing(listingId, UUID.randomUUID(), new BigDecimal("100.00"))));
        when(bidRepository.findTopByListingIdOrderByAmountDescCreatedAtAsc(listingId))
                .thenReturn(Optional.empty());
        when(bidRepository.findTopByListingIdAndBuyerIdOrderByCreatedAtDesc(listingId, buyerId))
                .thenReturn(Optional.empty());
        doThrow(new InsufficientBalanceException("Saldo tidak mencukupi."))
                .when(walletService).reserveBidFunds(buyerId, listingId, bidAmount);

        assertThrows(InsufficientBalanceException.class, () -> bidService.placeBid(buyerId, new CreateBidRequest(
                listingId, bidAmount, Boolean.FALSE, null)));

        verify(bidRepository, never()).save(any(Bid.class));
    }

    @Test
    void placeBidSuccessShouldReleasePreviousHighestBidderFunds() {
        UUID listingId = UUID.randomUUID();
        UUID previousBuyerId = UUID.randomUUID();
        UUID newBuyerId = UUID.randomUUID();
        BigDecimal previousAmount = new BigDecimal("200.00");
        BigDecimal newAmount = new BigDecimal("250.00");

        Bid previousHighest = new Bid();
        previousHighest.setId(UUID.randomUUID());
        previousHighest.setListingId(listingId);
        previousHighest.setBuyerId(previousBuyerId);
        previousHighest.setAmount(previousAmount);
        previousHighest.setProxyBid(Boolean.FALSE);
        previousHighest.setCreatedAt(LocalDateTime.now().minusMinutes(5));

        when(listingService.getListingById(listingId))
                .thenReturn(Optional.of(activeListing(listingId, UUID.randomUUID(), new BigDecimal("100.00"))));
        when(bidRepository.findTopByListingIdOrderByAmountDescCreatedAtAsc(listingId))
                .thenReturn(Optional.of(previousHighest));
        when(bidRepository.findTopByListingIdAndBuyerIdOrderByCreatedAtDesc(listingId, newBuyerId))
                .thenReturn(Optional.empty());
        when(bidRepository.save(any(Bid.class))).thenAnswer(inv -> savedBid(listingId, newBuyerId, newAmount));

        bidService.placeBid(newBuyerId, new CreateBidRequest(
                listingId, newAmount, Boolean.FALSE, null));

        // New buyer's funds should be locked.
        verify(walletService).reserveBidFunds(newBuyerId, listingId, newAmount);

        // Previous highest bidder's funds should be released.
        verify(walletService).releaseBidFunds(eq(previousBuyerId), eq(listingId), eq(previousAmount));
    }

    @Test
    void placeBidRaisingSameBuyerOnlyLocksAdditionalDelta() {
        UUID listingId = UUID.randomUUID();
        UUID buyerId = UUID.randomUUID();
        BigDecimal previousAmount = new BigDecimal("200.00");
        BigDecimal newAmount = new BigDecimal("300.00");

        // Same buyer previously bid 200.
        Bid previousByBuyer = new Bid();
        previousByBuyer.setId(UUID.randomUUID());
        previousByBuyer.setListingId(listingId);
        previousByBuyer.setBuyerId(buyerId);
        previousByBuyer.setAmount(previousAmount);
        previousByBuyer.setProxyBid(Boolean.FALSE);
        previousByBuyer.setCreatedAt(LocalDateTime.now().minusMinutes(3));

        when(listingService.getListingById(listingId))
                .thenReturn(Optional.of(activeListing(listingId, UUID.randomUUID(), new BigDecimal("100.00"))));
        when(bidRepository.findTopByListingIdOrderByAmountDescCreatedAtAsc(listingId))
                .thenReturn(Optional.of(previousByBuyer));
        when(bidRepository.findTopByListingIdAndBuyerIdOrderByCreatedAtDesc(listingId, buyerId))
                .thenReturn(Optional.of(previousByBuyer));
        when(bidRepository.save(any(Bid.class))).thenAnswer(inv -> savedBid(listingId, buyerId, newAmount));

        bidService.placeBid(buyerId, new CreateBidRequest(
                listingId, newAmount, Boolean.FALSE, null));

        // Only the delta (300 - 200 = 100) should be newly locked, not the full 300.
        verify(walletService).reserveBidFunds(buyerId, listingId, new BigDecimal("100.00"));

        // Self-outbid: no release for same buyer.
        verify(walletService, never()).releaseBidFunds(any(), any(), any());
    }
}
