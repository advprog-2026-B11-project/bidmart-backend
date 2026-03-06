package com.example.bidmart.listing.controller;

import com.example.bidmart.listing.model.Listing;
import com.example.bidmart.listing.service.ListingService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/listings")
public class ListingController {

    private final ListingService listingService;

    public ListingController(ListingService listingService) {
        this.listingService = listingService;
    }

    @PostMapping
    public ResponseEntity<Listing> createListing(@RequestBody Listing listing) {
        Listing created = listingService.createListing(listing);
        return ResponseEntity.ok(created);
    }

    @GetMapping
    public ResponseEntity<List<Listing>> getAllListings() {
        return ResponseEntity.ok(listingService.getAllListings());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Listing> getListingById(@PathVariable UUID id) {
        return listingService.getListingById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}