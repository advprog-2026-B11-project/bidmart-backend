package com.example.bidmart.listing.controller;

import com.example.bidmart.common.validation.OnCreate;
import com.example.bidmart.listing.dto.CreateListingRequest;
import com.example.bidmart.listing.dto.ListingResponse;
import com.example.bidmart.listing.dto.PaginatedResponse;
import com.example.bidmart.listing.service.ListingService;
import com.example.bidmart.user.service.UserService;
import jakarta.validation.groups.Default;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;


import java.math.BigDecimal;
import java.util.UUID;

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

    @PreAuthorize("hasRole('SELLER') or hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<ListingResponse> createListing(
            @Validated({Default.class, OnCreate.class}) @RequestBody CreateListingRequest request,
            Authentication authentication
    ) {
        UUID sellerId = resolveCurrentUserId(authentication);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ListingResponse.from(listingService.createListing(request, sellerId)));
    }

    @GetMapping
    public ResponseEntity<PaginatedResponse<ListingResponse>> getAllListings(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        return ResponseEntity.ok(PaginatedResponse.from(
                listingService.getAllListings(createPageable(page, size))
                        .map(ListingResponse::from)));
    }

    @GetMapping("/active")
    public ResponseEntity<PaginatedResponse<ListingResponse>> getActiveListings(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        return ResponseEntity.ok(PaginatedResponse.from(
                listingService.getActiveListings(createPageable(page, size))
                        .map(ListingResponse::from)));
    }

    @GetMapping("/search")
    public ResponseEntity<PaginatedResponse<ListingResponse>> searchListings(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "category", required = false) String category,
            @RequestParam(name = "minPrice", required = false) BigDecimal minPrice,
            @RequestParam(name = "maxPrice", required = false) BigDecimal maxPrice,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        return ResponseEntity.ok(PaginatedResponse.from(
                listingService.searchListings(keyword, category, minPrice, maxPrice, createPageable(page, size))
                        .map(ListingResponse::from)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ListingResponse> getListingById(@PathVariable("id") UUID id) {
        return listingService.getListingById(id)
                .map(ListingResponse::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasRole('SELLER') or hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<?> updateListing(
            @PathVariable("id") UUID id,
            @Validated({Default.class, OnCreate.class}) @RequestBody CreateListingRequest request,
            Authentication authentication) {
        UUID requesterId = resolveCurrentUserId(authentication);
        try {
            return ResponseEntity.ok(ListingResponse.from(
                    listingService.updateListing(id, request, requesterId)));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PreAuthorize("hasRole('SELLER') or hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteListing(
            @PathVariable("id") UUID id,
            Authentication authentication) {
        UUID requesterId = resolveCurrentUserId(authentication);
        try {
            listingService.deleteListing(id, requesterId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
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
}
