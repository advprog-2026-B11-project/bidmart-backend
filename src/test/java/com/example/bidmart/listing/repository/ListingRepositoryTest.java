package com.example.bidmart.listing.repository;

import com.example.bidmart.listing.model.AuctionStatus;
import com.example.bidmart.listing.model.AuctionType;
import com.example.bidmart.listing.model.Listing;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class ListingRepositoryTest {

    @Autowired
    private ListingRepository listingRepository;

    @BeforeEach
    void setUp() {
        listingRepository.deleteAll();
    }

    @Test
    void findBySearchCriteria_shouldFilterByKeyword() {
        Listing camera = listing("Vintage Camera", "Analog collectible", new BigDecimal("500"), AuctionStatus.ACTIVE);
        Listing laptop = listing("Laptop", "Work device", new BigDecimal("900"), AuctionStatus.ACTIVE);
        listingRepository.saveAll(List.of(camera, laptop));

        List<Listing> result = listingRepository.findBySearchCriteria("camera", null, null, null);

        assertEquals(1, result.size());
        assertEquals("Vintage Camera", result.get(0).getTitle());
    }

    @Test
    void findBySearchCriteria_shouldFilterByCategory() {
        UUID targetCategory = UUID.randomUUID();
        Listing match = listing("Phone", "Android", new BigDecimal("300"), AuctionStatus.ACTIVE);
        Listing other = listing("Shoes", "Running", new BigDecimal("120"), AuctionStatus.ACTIVE);
        match.setCategoryId(targetCategory);
        listingRepository.saveAll(List.of(match, other));

        List<Listing> result = listingRepository.findBySearchCriteria(null, targetCategory, null, null);

        assertEquals(1, result.size());
        assertEquals(targetCategory, result.get(0).getCategoryId());
    }

    @Test
    void findBySearchCriteria_shouldFilterByPriceRange() {
        Listing cheap = listing("Cheap Item", "Budget", new BigDecimal("50"), AuctionStatus.ACTIVE);
        Listing target = listing("Target Item", "Middle", new BigDecimal("150"), AuctionStatus.ACTIVE);
        Listing expensive = listing("Expensive Item", "Premium", new BigDecimal("500"), AuctionStatus.ACTIVE);
        listingRepository.saveAll(List.of(cheap, target, expensive));

        List<Listing> result = listingRepository.findBySearchCriteria(null, null, new BigDecimal("100"), new BigDecimal("200"));

        assertEquals(1, result.size());
        assertEquals("Target Item", result.get(0).getTitle());
    }

    @Test
    void findActiveListings_shouldReturnActiveAndExtendedOnly() {
        Listing active = listing("Active", "Open", new BigDecimal("100"), AuctionStatus.ACTIVE);
        Listing extended = listing("Extended", "Open", new BigDecimal("200"), AuctionStatus.EXTENDED);
        Listing closed = listing("Closed", "Done", new BigDecimal("300"), AuctionStatus.CLOSED);
        listingRepository.saveAll(List.of(active, extended, closed));

        List<Listing> result = listingRepository.findActiveListings();

        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(listing -> listing.getStatus() == AuctionStatus.ACTIVE));
        assertTrue(result.stream().anyMatch(listing -> listing.getStatus() == AuctionStatus.EXTENDED));
        assertTrue(result.stream().noneMatch(listing -> listing.getStatus() == AuctionStatus.CLOSED));
    }

    private Listing listing(String title, String description, BigDecimal startingPrice, AuctionStatus status) {
        Listing listing = new Listing();
        listing.setSellerId(UUID.randomUUID());
        listing.setCategoryId(UUID.randomUUID());
        listing.setTitle(title);
        listing.setDescription(description);
        listing.setStartingPrice(startingPrice);
        listing.setEndTime(LocalDateTime.now().plusDays(1));
        listing.setStatus(status);
        listing.setAuctionType(AuctionType.ENGLISH);
        return listing;
    }
}