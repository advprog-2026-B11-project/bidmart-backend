package com.example.bidmart.listing.service;

import com.example.bidmart.listing.model.Listing;
import com.example.bidmart.listing.repository.ListingRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ListingService {

    private final ListingRepository listingRepository;

    public ListingService(ListingRepository listingRepository) {
        this.listingRepository = listingRepository;
    }

    public Listing createListing(Listing listing, UUID sellerId) {
        listing.setSellerId(sellerId);
        listing.setCreatedAt(LocalDateTime.now());

        if (listing.getStatus() == null || listing.getStatus().isBlank()) {
            listing.setStatus("ACTIVE");
        }

        return listingRepository.save(listing);
    }

    public List<Listing> getAllListings() {
        return listingRepository.findAll();
    }

    public Optional<Listing> getListingById(UUID id) {
        return listingRepository.findById(id);
    }
}
