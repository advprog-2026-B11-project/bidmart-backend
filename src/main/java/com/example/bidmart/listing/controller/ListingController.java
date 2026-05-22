package com.example.bidmart.listing.controller;

import com.example.bidmart.common.validation.OnCreate;
import com.example.bidmart.listing.dto.CreateListingRequest;
import com.example.bidmart.listing.dto.ListingResponse;
import com.example.bidmart.listing.dto.PaginatedResponse;
import com.example.bidmart.listing.service.ListingService;
import com.example.bidmart.user.service.UserService;
import jakarta.validation.groups.Default;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

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

    @PreAuthorize("hasAuthority(T(com.example.bidmart.common.security.PermissionNames).LISTING_CREATE)")
    @PostMapping
    public ResponseEntity<ListingResponse> createListing(
            @Validated({Default.class, OnCreate.class}) @RequestBody CreateListingRequest request,
            Authentication authentication) {
        UUID sellerId = resolveCurrentUserId(authentication);
        return ResponseEntity.ok(ListingResponse.from(listingService.createListing(request, sellerId)));
    }
    
    @PreAuthorize("hasAuthority(T(com.example.bidmart.common.security.PermissionNames).LISTING_READ)")
    @GetMapping
    public ResponseEntity<PaginatedResponse<ListingResponse>> getAllListings(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        return ResponseEntity.ok(PaginatedResponse.from(
                listingService.getAllListings(createPageable(page, size)).map(ListingResponse::from)));
    }

    @GetMapping("/active")
    public ResponseEntity<PaginatedResponse<ListingResponse>> getActiveListings(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        return ResponseEntity.ok(PaginatedResponse.from(
                listingService.getActiveListings(createPageable(page, size)).map(ListingResponse::from)));
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchListings(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "category", required = false) String category,
            @RequestParam(name = "minPrice", required = false) BigDecimal minPrice,
            @RequestParam(name = "maxPrice", required = false) BigDecimal maxPrice,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        try {
            return ResponseEntity.ok(PaginatedResponse.from(
                    listingService.searchListings(keyword, category, minPrice, maxPrice, createPageable(page, size))
                            .map(ListingResponse::from)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority(T(com.example.bidmart.common.security.PermissionNames).LISTING_READ)")
    public ResponseEntity<ListingResponse> getListingById(@PathVariable("id") UUID id) {
        return listingService.getListingById(id)
                .map(ListingResponse::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasAuthority(T(com.example.bidmart.common.security.PermissionNames).LISTING_UPDATE)")
    @PutMapping("/{id}")
    public ResponseEntity<?> updateListing(
            @PathVariable("id") UUID id,
            @Validated({Default.class, OnCreate.class}) @RequestBody CreateListingRequest request,
            Authentication authentication) {
        try {
            UUID requesterId = resolveCurrentUserId(authentication);
            return ResponseEntity.ok(ListingResponse.from(
                    listingService.updateListing(id, request, requesterId, isAdmin(authentication))));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PreAuthorize("hasAuthority(T(com.example.bidmart.common.security.PermissionNames).LISTING_DELETE)")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteListing(@PathVariable("id") UUID id, Authentication authentication) {
        try {
            UUID requesterId = resolveCurrentUserId(authentication);
            listingService.deleteListing(id, requesterId, isAdmin(authentication));
            return ResponseEntity.noContent().build();
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
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

    private boolean isAdmin(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }

    private Pageable createPageable(int page, int size) {
        int resolvedPage = Math.max(page, 0);
        int resolvedSize = Math.min(Math.max(size, 1), 100);
        return PageRequest.of(resolvedPage, resolvedSize, Sort.by(Sort.Direction.DESC, "createdAt"));
    }
}