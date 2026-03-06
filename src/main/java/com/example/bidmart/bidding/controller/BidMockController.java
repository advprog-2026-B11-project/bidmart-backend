package com.example.bidmart.bidding.controller;

import com.example.bidmart.bidding.dto.MockListingUpsertRequest;
import com.example.bidmart.bidding.dto.MockWalletBalanceRequest;
import com.example.bidmart.bidding.dto.MockWalletStateResponse;
import com.example.bidmart.bidding.service.ListingSnapshot;
import com.example.bidmart.bidding.service.MockListingService;
import com.example.bidmart.bidding.service.MockWalletService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bids/mocks")
public class BidMockController {

    private final MockListingService mockListingService;
    private final MockWalletService mockWalletService;

    public BidMockController(MockListingService mockListingService, MockWalletService mockWalletService) {
        this.mockListingService = mockListingService;
        this.mockWalletService = mockWalletService;
    }

    @GetMapping("/listings")
    public List<ListingSnapshot> getMockListings() {
        return mockListingService.findAll();
    }

    @PostMapping("/listings")
    public ResponseEntity<ListingSnapshot> upsertMockListing(@RequestBody MockListingUpsertRequest request) {
        ListingSnapshot listingSnapshot = mockListingService.upsert(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(listingSnapshot);
    }

    @PutMapping("/wallets/{buyerId}")
    public MockWalletStateResponse upsertMockWalletBalance(
            @PathVariable UUID buyerId,
            @RequestBody MockWalletBalanceRequest request
    ) {
        mockWalletService.setAvailableBalance(buyerId, request.availableBalance());
        return mockWalletService.getWalletState(buyerId);
    }

    @GetMapping("/wallets/{buyerId}")
    public MockWalletStateResponse getMockWalletState(@PathVariable UUID buyerId) {
        return mockWalletService.getWalletState(buyerId);
    }
}
