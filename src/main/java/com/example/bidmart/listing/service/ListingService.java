package com.example.bidmart.listing.service;

import com.example.bidmart.listing.dto.CreateListingRequest;
import com.example.bidmart.listing.dto.UpdateListingRequest;
import com.example.bidmart.listing.model.AuctionType;
import com.example.bidmart.listing.model.AuctionStatus;
import com.example.bidmart.listing.model.Listing;
import com.example.bidmart.listing.repository.ListingRepository;
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

    public Listing createListing(CreateListingRequest request, UUID sellerId) {
        validateReservePrice(request.startingPrice(), request.reservePrice());

        Listing listing = new Listing();
        listing.setSellerId(sellerId);
        listing.setCategoryId(request.categoryId());
        listing.setTitle(request.title());
        listing.setDescription(request.description());
        listing.setImageUrl(request.imageUrl());
        listing.setStartingPrice(request.startingPrice());
        listing.setReservePrice(request.reservePrice());
        listing.setEndTime(request.endTime());
        listing.setStatus(AuctionStatus.ACTIVE);
        listing.setAuctionType(request.auctionType() == null ? AuctionType.ENGLISH : request.auctionType());
        listing.setCreatedAt(LocalDateTime.now());

        return listingRepository.save(listing);
    }

    private void validateReservePrice(BigDecimal startingPrice, BigDecimal reservePrice) {
        if (reservePrice != null && startingPrice != null && reservePrice.compareTo(startingPrice) < 0) {
            throw new IllegalArgumentException("Reserve price tidak boleh lebih kecil dari starting price.");
        }
    }

    public List<Listing> getAllListings() {
        return listingRepository.findAll();
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

    public Listing updateListing(UUID id, UpdateListingRequest request) {
        Listing existing = listingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Listing tidak ditemukan dengan ID: " + id));

        validateListingNotActive(existing, "diupdate");
        validateReservePrice(request.startingPrice(), request.reservePrice());

        existing.setCategoryId(request.categoryId());
        existing.setTitle(request.title());
        existing.setDescription(request.description());
        existing.setImageUrl(request.imageUrl());
        existing.setStartingPrice(request.startingPrice());
        existing.setReservePrice(request.reservePrice());
        existing.setEndTime(request.endTime());
        existing.setAuctionType(request.auctionType() == null ? AuctionType.ENGLISH : request.auctionType());
        return listingRepository.save(existing);
    }

    private void validateListingNotActive(Listing listing, String action) {
        if (listing.getStatus() != null && listing.getStatus().isActive()) {
            throw new IllegalArgumentException("Listing tidak bisa " + action + " saat auction masih aktif.");
        }
    }

    public void deleteListing(UUID id) {
        Listing existing = listingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Listing tidak ditemukan dengan ID: " + id));

        validateListingNotActive(existing, "dihapus");

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

    public List<Listing> getActiveListings() {
        return listingRepository.findActiveListings();
    }
}