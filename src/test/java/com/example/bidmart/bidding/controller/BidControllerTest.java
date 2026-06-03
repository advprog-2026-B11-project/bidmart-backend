package com.example.bidmart.bidding.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.example.bidmart.bidding.dto.BidResponse;
import com.example.bidmart.bidding.dto.CreateBidRequest;
import com.example.bidmart.bidding.service.BidService;
import com.example.bidmart.user.service.UserService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
class BidControllerTest {

    @Mock private BidService bidService;
    @Mock private UserService userService;
    @InjectMocks private BidController bidController;

    private UUID buyerId;
    private UUID listingId;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        buyerId   = UUID.randomUUID();
        listingId = UUID.randomUUID();
        authentication = mock(Authentication.class);
        lenient().when(authentication.getName()).thenReturn("buyerUser");
        lenient().when(userService.getUserIdByUsername("buyerUser")).thenReturn(buyerId);
    }

    private BidResponse sampleResponse() {
        return new BidResponse(UUID.randomUUID(), listingId, buyerId, new BigDecimal("250.00"), false, null, LocalDateTime.now());
    }

    // ── placeBid ─────────────────────────────────────────────────────────────

    @Test
    void placeBid_shouldReturn201() {
        CreateBidRequest request = new CreateBidRequest(listingId, new BigDecimal("250.00"), false, null);
        when(bidService.placeBid(buyerId, request)).thenReturn(sampleResponse());

        ResponseEntity<BidResponse> response = bidController.placeBid(request, authentication);

        assertEquals(201, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(listingId, response.getBody().listingId());
    }

    @Test
    void placeBid_nullAuthentication_throws() {
        CreateBidRequest request = new CreateBidRequest(listingId, new BigDecimal("100.00"), false, null);
        assertThrows(AccessDeniedException.class, () -> bidController.placeBid(request, null));
    }

    @Test
    void placeBid_nullAuthName_throws() {
        Authentication noName = mock(Authentication.class);
        when(noName.getName()).thenReturn(null);
        CreateBidRequest request = new CreateBidRequest(listingId, new BigDecimal("100.00"), false, null);
        assertThrows(AccessDeniedException.class, () -> bidController.placeBid(request, noName));
    }

    // ── getBidsByListing ──────────────────────────────────────────────────────

    @Test
    void getBidsByListing_shouldReturnList() {
        when(bidService.getBidsByListing(listingId, buyerId)).thenReturn(List.of(sampleResponse()));

        List<BidResponse> result = bidController.getBidsByListing(listingId, authentication);

        assertEquals(1, result.size());
        assertEquals(new BigDecimal("250.00"), result.get(0).amount());
    }

    // ── getHighestBid ─────────────────────────────────────────────────────────

    @Test
    void getHighestBid_shouldReturnHighestBid() {
        BidResponse highest = sampleResponse();
        when(bidService.getHighestBid(listingId, buyerId)).thenReturn(highest);

        BidResponse result = bidController.getHighestBid(listingId, authentication);

        assertNotNull(result);
        assertEquals(highest.id(), result.id());
    }

    // ── getMinimumNextBid ─────────────────────────────────────────────────────

    @Test
    void getMinimumNextBid_shouldReturnAmount() {
        BigDecimal minimum = new BigDecimal("251.00");
        when(bidService.getMinimumNextBid(listingId)).thenReturn(minimum);

        ResponseEntity<BigDecimal> response = bidController.getMinimumNextBid(listingId);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(0, minimum.compareTo(response.getBody()));
    }

    // ── getMyBids ─────────────────────────────────────────────────────────────

    @Test
    void getMyBids_shouldReturnBuyerBids() {
        when(bidService.getBidsByBuyer(buyerId)).thenReturn(List.of(sampleResponse()));

        List<BidResponse> result = bidController.getMyBids(authentication);

        assertEquals(1, result.size());
    }

    @Test
    void getMyBids_nullAuthentication_throws() {
        assertThrows(AccessDeniedException.class, () -> bidController.getMyBids(null));
    }

    // ── getBidsByBuyer ────────────────────────────────────────────────────────

    @Test
    void getBidsByBuyer_sameUser_shouldReturnBids() {
        when(bidService.getBidsByBuyer(buyerId)).thenReturn(List.of(sampleResponse()));

        List<BidResponse> result = bidController.getBidsByBuyer(buyerId, authentication);

        assertEquals(1, result.size());
    }

    @Test
    void getBidsByBuyer_differentUser_throwsAccessDenied() {
        UUID otherUser = UUID.randomUUID();
        assertThrows(AccessDeniedException.class,
                () -> bidController.getBidsByBuyer(otherUser, authentication));
    }
}
