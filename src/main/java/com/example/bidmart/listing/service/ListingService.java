package com.example.bidmart.listing.service;

import com.example.bidmart.listing.dto.CreateListingRequest;
import com.example.bidmart.listing.model.AuctionStatus;
import com.example.bidmart.listing.model.AuctionType;
import com.example.bidmart.listing.model.Listing;
import com.example.bidmart.listing.repository.ListingRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public Listing updateListing(UUID id, CreateListingRequest request, UUID requesterId, boolean admin) {
        Listing existing = listingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Listing tidak ditemukan dengan ID: " + id));

        validateListingOwnership(existing, requesterId, admin);
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

    public void deleteListing(UUID id, UUID requesterId, boolean admin) {
        Listing existing = listingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Listing tidak ditemukan dengan ID: " + id));

        validateListingOwnership(existing, requesterId, admin);
        validateListingNotActive(existing, "dihapus");
        listingRepository.delete(existing);
    }

    public List<Listing> searchListings(String keyword, String category, BigDecimal minPrice, BigDecimal maxPrice) {
        validatePriceRange(minPrice, maxPrice);
        return listingRepository.findBySearchCriteria(normalizeKeyword(keyword), parseCategoryId(category), minPrice, maxPrice);
    }

    public Page<Listing> searchListings(String keyword, String category, BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable) {
        validatePriceRange(minPrice, maxPrice);
        Pageable unsorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        return listingRepository.findBySearchCriteria(normalizeKeyword(keyword), parseCategoryId(category), minPrice, maxPrice, unsorted);
    }

    public List<Listing> getActiveListings() {
        return listingRepository.findActiveListings();
    }

    public Page<Listing> getActiveListings(Pageable pageable) {
        return listingRepository.findActiveListings(pageable);
    }

    private void validateReservePrice(BigDecimal startingPrice, BigDecimal reservePrice) {
        if (reservePrice != null && startingPrice != null && reservePrice.compareTo(startingPrice) < 0) {
            throw new IllegalArgumentException("Reserve price tidak boleh lebih kecil dari starting price.");
        }
    }

    private void validatePriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        if (minPrice != null && minPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Minimum price tidak boleh negatif.");
        }
        if (maxPrice != null && maxPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Maximum price tidak boleh negatif.");
        }
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new IllegalArgumentException("Minimum price tidak boleh lebih besar dari maximum price.");
        }
    }

    private void validateListingNotActive(Listing listing, String action) {
        if (listing.getStatus() != null && listing.getStatus().isActive()) {
            throw new IllegalArgumentException("Listing tidak bisa " + action + " saat auction masih aktif.");
        }
    }

    private void validateListingOwnership(Listing listing, UUID requesterId, boolean admin) {
        if (!admin && !listing.getSellerId().equals(requesterId)) {
            throw new AccessDeniedException("User tidak memiliki akses ke listing ini.");
        }
    }

    private String normalizeKeyword(String keyword) {
        return keyword == null || keyword.isBlank() ? null : keyword.trim();
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