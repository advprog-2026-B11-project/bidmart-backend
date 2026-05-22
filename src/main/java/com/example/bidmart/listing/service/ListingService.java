package com.example.bidmart.listing.service;

import com.example.bidmart.listing.dto.CreateListingRequest;
import com.example.bidmart.listing.model.AuctionStatus;
import com.example.bidmart.listing.model.AuctionType;
import com.example.bidmart.listing.model.Listing;
import com.example.bidmart.listing.repository.ListingRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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

    public Listing createListing(CreateListingRequest request, UUID sellerId) {
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
        listing.setAuctionType(AuctionType.ENGLISH);
        listing.setCreatedAt(LocalDateTime.now());
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

    public Listing updateListing(UUID id, CreateListingRequest request, UUID requesterId) {
        Listing existing = listingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Listing tidak ditemukan dengan ID: " + id));

        if (!existing.getSellerId().equals(requesterId)) {
            throw new RuntimeException("Hanya seller pemilik listing yang dapat mengubah data ini.");
        }

        if (existing.getStatus() != null && existing.getStatus().isActive()) {
            throw new RuntimeException("Listing tidak bisa diupdate saat auction masih aktif.");
        }

        existing.setCategoryId(request.categoryId());
        existing.setTitle(request.title());
        existing.setDescription(request.description());
        existing.setImageUrl(request.imageUrl());
        existing.setStartingPrice(request.startingPrice());
        existing.setReservePrice(request.reservePrice());
        existing.setEndTime(request.endTime());
        return listingRepository.save(existing);
    }

    public void deleteListing(UUID id, UUID requesterId) {
        Listing existing = listingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Listing tidak ditemukan dengan ID: " + id));

        if (!existing.getSellerId().equals(requesterId)) {
            throw new RuntimeException("Hanya seller pemilik listing yang dapat menghapus data ini.");
        }

        if (existing.getStatus() != null && existing.getStatus().isActive()) {
            throw new RuntimeException("Listing tidak bisa dihapus saat auction masih aktif.");
        }

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
        // Native query has its own ORDER BY — strip sort from pageable to prevent camelCase column clash
        Pageable unsorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        return listingRepository.findBySearchCriteria(keyword, categoryId, minPrice, maxPrice, unsorted);
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
