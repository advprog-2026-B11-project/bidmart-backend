package com.example.bidmart.bidding.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import com.example.bidmart.bidding.dto.BidResponse;
import com.example.bidmart.bidding.dto.CreateBidRequest;
import com.example.bidmart.bidding.service.BidService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class BidControllerTest {

    @Mock
    private BidService bidService;

    @InjectMocks
    private BidController bidController;

    @Test
    void placeBidShouldReturnCreated() {
        UUID bidId = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();
        UUID buyerId = UUID.randomUUID();

        CreateBidRequest request = new CreateBidRequest(listingId, buyerId, new BigDecimal("250.00"), Boolean.FALSE, null);
        BidResponse responsePayload = new BidResponse(
                bidId,
                listingId,
                buyerId,
                new BigDecimal("250.00"),
                Boolean.FALSE,
                null,
                LocalDateTime.now()
        );

        when(bidService.placeBid(request)).thenReturn(responsePayload);

        ResponseEntity<BidResponse> response = bidController.placeBid(request);

        assertEquals(201, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(bidId, response.getBody().id());
    }

    @Test
    void getBidsByListingShouldReturnBidHistory() {
        UUID listingId = UUID.randomUUID();

        List<BidResponse> bids = List.of(new BidResponse(
                UUID.randomUUID(),
                listingId,
                UUID.randomUUID(),
                new BigDecimal("300.00"),
                Boolean.FALSE,
                null,
                LocalDateTime.now()
        ));

        when(bidService.getBidsByListing(listingId)).thenReturn(bids);

        List<BidResponse> result = bidController.getBidsByListing(listingId);

        assertEquals(1, result.size());
        assertEquals(new BigDecimal("300.00"), result.get(0).amount());
    }
}
