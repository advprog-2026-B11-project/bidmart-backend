package com.example.bidmart.bidding.service;

import com.example.bidmart.listing.model.Listing;
import com.example.bidmart.listing.repository.ListingRepository;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

@Service
public class ListingLookupService {

    private final ListingRepository listingRepository;
    private final MockListingService mockListingService;

    public ListingLookupService(ListingRepository listingRepository, MockListingService mockListingService) {
        this.listingRepository = listingRepository;
        this.mockListingService = mockListingService;
    }

    public Optional<ListingSnapshot> findById(UUID listingId) {
        try {
            Optional<ListingSnapshot> listingFromRepository = listingRepository.findById(listingId).map(this::toSnapshot);
            if (listingFromRepository.isPresent()) {
                return listingFromRepository;
            }
        } catch (DataAccessException ex) {
            // Fallback ke mock listing saat modul listing/database belum siap.
        }

        return mockListingService.findById(listingId);
    }

    private ListingSnapshot toSnapshot(Listing listing) {
        BigDecimal startingPrice = listing.getStartingPrice() == null ? BigDecimal.ZERO : listing.getStartingPrice();

        return new ListingSnapshot(
                listing.getId(),
                listing.getSellerId(),
                startingPrice,
                listing.getEndTime(),
                listing.getStatus()
        );
    }
}
