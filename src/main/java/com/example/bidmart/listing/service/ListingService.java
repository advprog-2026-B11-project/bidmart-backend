package com.example.bidmart.listing.service;

import com.example.bidmart.listing.model.AuctionStatus;
import com.example.bidmart.listing.model.Listing;
import com.example.bidmart.listing.repository.ListingRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.math.BigDecimal;

@Service
public class ListingService {

    private final ListingRepository listingRepository;

    public ListingService(ListingRepository listingRepository) {
        this.listingRepository = listingRepository;
    }

    public Listing createListing(Listing listing, UUID sellerId) {
        listing.setSellerId(sellerId);
        listing.setCreatedAt(LocalDateTime.now());

        if (listing.getStatus() == null) {
            listing.setStatus(AuctionStatus.ACTIVE);
        }

        return listingRepository.save(listing);
    }

    public List<Listing> getAllListings() {
        return listingRepository.findAll();
    }

    public Page<Listing> getAllListings(Pageable pageable) {
        return listingRepository.findAll(pageable);
    }

    public Optional<Listing> getListingById(UUID id) {
        return listingRepository.findById(id);
    }

    @Transactional
    public Optional<Listing> getListingByIdWithLock(UUID id) {
        return listingRepository.findByIdWithLock(id);
    }

    @Transactional
    public Listing save(Listing listing) {
        return listingRepository.save(listing);
    }

    public Listing updateListing(UUID id, Listing updatedListing) {
        Listing existing = listingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Listing tidak ditemukan dengan ID: " + id));

        validateListingForUpdate(existing);

        existing.setTitle(updatedListing.getTitle());
        existing.setDescription(updatedListing.getDescription());
        existing.setImageUrl(updatedListing.getImageUrl());
        existing.setStartingPrice(updatedListing.getStartingPrice());
        existing.setReservePrice(updatedListing.getReservePrice());
        existing.setEndTime(updatedListing.getEndTime());
        return listingRepository.save(existing);
    }

    private void validateListingForUpdate(Listing listing) {
        if (listing.getStatus() != null && listing.getStatus().isActive()) {
            throw new RuntimeException(
                    "Listing tidak bisa diupdate saat auction masih aktif.");
        }
    }

    public void deleteListing(UUID id) {
        Listing existing = listingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Listing tidak ditemukan dengan ID: " + id));

        validateListingForUpdate(existing);

        listingRepository.delete(existing);
    }

    public List<Listing> searchListings(String keyword, String category, BigDecimal minPrice, BigDecimal maxPrice) {
        UUID categoryId = null;
        if (category != null && !category.isEmpty()) {
            try {
                categoryId = UUID.fromString(category);
            } catch (IllegalArgumentException e) {
                // Invalid UUID, ignore category filter
            }
        }
        return listingRepository.findBySearchCriteria(keyword, categoryId, minPrice, maxPrice);
    }

    public Page<Listing> searchListings(String keyword, String category, BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable) {
        UUID categoryId = parseCategoryId(category);
        return listingRepository.findBySearchCriteria(keyword, categoryId, minPrice, maxPrice, pageable);
    }

    public List<Listing> getActiveListings() {
        return listingRepository.findActiveListings();
    }

    public Page<Listing> getActiveListings(Pageable pageable) {
        return listingRepository.findActiveListings(pageable);
    }

    private UUID parseCategoryId(String category) {
        if (category == null || category.isEmpty()) {
            return null;
        }

        try {
            return UUID.fromString(category);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
