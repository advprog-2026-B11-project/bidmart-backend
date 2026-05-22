package com.example.bidmart.listing.controller;

import com.example.bidmart.listing.dto.PaginatedResponse;
import com.example.bidmart.listing.model.Listing;
import com.example.bidmart.listing.service.ListingService;
import com.example.bidmart.user.service.UserService;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import java.math.BigDecimal;
import jakarta.validation.Valid;
import jakarta.validation.groups.Default;
import com.example.bidmart.common.validation.OnCreate;

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
            @Validated({Default.class, OnCreate.class}) @RequestBody Listing listing,
            Authentication authentication
    ) {
        UUID sellerId = resolveCurrentUserId(authentication);
        Listing created = listingService.createListing(listing, sellerId);
        return ResponseEntity.ok(created);
    }

    @GetMapping
    public ResponseEntity<PaginatedResponse<Listing>> getAllListings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(PaginatedResponse.from(listingService.getAllListings(createPageable(page, size))));
    }

    @GetMapping("/search")
    public ResponseEntity<PaginatedResponse<Listing>> searchListings(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(PaginatedResponse.from(
                listingService.searchListings(keyword, category, minPrice, maxPrice, createPageable(page, size))));
    }

    @GetMapping("/active")
    public ResponseEntity<PaginatedResponse<Listing>> getActiveListings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(PaginatedResponse.from(listingService.getActiveListings(createPageable(page, size))));
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

    private Pageable createPageable(int page, int size) {
        int resolvedPage = Math.max(page, 0);
        int resolvedSize = Math.min(Math.max(size, 1), 100);
        return PageRequest.of(resolvedPage, resolvedSize, Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateListing(@PathVariable UUID id, @Valid @RequestBody Listing listing) {
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
