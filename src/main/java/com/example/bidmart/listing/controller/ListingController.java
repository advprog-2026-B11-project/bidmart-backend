package com.example.bidmart.listing.controller;

import com.example.bidmart.listing.model.Listing;
import com.example.bidmart.listing.service.ListingService;
import com.example.bidmart.user.service.UserService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.security.access.prepost.PreAuthorize;
import java.math.BigDecimal;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/listings")
public class ListingController {

    private final ListingService listingService;
    private final UserService userService;

    public ListingController(ListingService listingService, UserService userService) {
        this.listingService = listingService;
        this.userService = userService;
    }

    @PreAuthorize("hasRole('SELLER') or hasRole('ADMIN')") @PostMapping
    public ResponseEntity<Listing> createListing(
            @RequestBody Listing listing,
            Authentication authentication
    ) {
        UUID sellerId = resolveCurrentUserId(authentication);
        Listing created = listingService.createListing(listing, sellerId);
        return ResponseEntity.ok(created);
    }

    @GetMapping
    public ResponseEntity<List<Listing>> getAllListings() {
        return ResponseEntity.ok(listingService.getAllListings());
    }

    @GetMapping("/search")
    public ResponseEntity<List<Listing>> searchListings(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice
    ) {
        List<Listing> results = listingService.searchListings(keyword, category, minPrice, maxPrice);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/active")
    public ResponseEntity<List<Listing>> getActiveListings() {
        List<Listing> activeListings = listingService.getActiveListings();
        return ResponseEntity.ok(activeListings);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Listing> getListingById(@PathVariable UUID id) {
        return listingService.getListingById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    private UUID resolveCurrentUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User belum terautentikasi.");
        }

        return userService.getUserIdByUsername(authentication.getName());
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateListing(@PathVariable UUID id, @RequestBody Listing listing) {
        try {
            Listing updated = listingService.updateListing(id, listing);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteListing(@PathVariable UUID id) {
        try {
            listingService.deleteListing(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
