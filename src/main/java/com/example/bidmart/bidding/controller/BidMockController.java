package com.example.bidmart.bidding.controller;

import com.example.bidmart.bidding.dto.MockListingUpsertRequest;
import com.example.bidmart.bidding.dto.MockWalletBalanceRequest;
import com.example.bidmart.bidding.dto.MockWalletStateResponse;
import com.example.bidmart.bidding.service.ListingSnapshot;
import com.example.bidmart.bidding.service.MockListingService;
import com.example.bidmart.bidding.service.MockWalletService;
import com.example.bidmart.user.service.UserService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
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
    private final UserService userService;

    public BidMockController(
            MockListingService mockListingService,
            MockWalletService mockWalletService,
            UserService userService
    ) {
        this.mockListingService = mockListingService;
        this.mockWalletService = mockWalletService;
        this.userService = userService;
    }

    @GetMapping("/listings")
    public List<ListingSnapshot> getMockListings() {
        return mockListingService.findAll();
    }

    @PostMapping("/listings")
    public ResponseEntity<ListingSnapshot> upsertMockListing(
            @RequestBody MockListingUpsertRequest request,
            Authentication authentication
    ) {
        UUID sellerId = resolveCurrentUserId(authentication);
        ListingSnapshot listingSnapshot = mockListingService.upsert(request, sellerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(listingSnapshot);
    }

    @PutMapping("/wallets/me")
    public MockWalletStateResponse upsertMockWalletBalance(
            @RequestBody MockWalletBalanceRequest request,
            Authentication authentication
    ) {
        UUID buyerId = resolveCurrentUserId(authentication);
        mockWalletService.setAvailableBalance(buyerId, request.availableBalance());
        return mockWalletService.getWalletState(buyerId);
    }

    @GetMapping("/wallets/me")
    public MockWalletStateResponse getMyMockWalletState(Authentication authentication) {
        UUID buyerId = resolveCurrentUserId(authentication);
        return mockWalletService.getWalletState(buyerId);
    }

    @PutMapping("/wallets/{buyerId}")
    public MockWalletStateResponse upsertMockWalletBalanceByBuyer(
            @PathVariable UUID buyerId,
            @RequestBody MockWalletBalanceRequest request,
            Authentication authentication
    ) {
        ensureCurrentUser(buyerId, authentication);
        mockWalletService.setAvailableBalance(buyerId, request.availableBalance());
        return mockWalletService.getWalletState(buyerId);
    }

    @GetMapping("/wallets/{buyerId}")
    public MockWalletStateResponse getMockWalletState(
            @PathVariable UUID buyerId,
            Authentication authentication
    ) {
        ensureCurrentUser(buyerId, authentication);
        return mockWalletService.getWalletState(buyerId);
    }

    private void ensureCurrentUser(UUID userId, Authentication authentication) {
        UUID authenticatedUserId = resolveCurrentUserId(authentication);
        if (!authenticatedUserId.equals(userId)) {
            throw new AccessDeniedException("Anda tidak dapat mengakses wallet mock milik user lain.");
        }
    }

    private UUID resolveCurrentUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new AccessDeniedException("User belum terautentikasi.");
        }

        return userService.getUserIdByUsername(authentication.getName());
    }
}
