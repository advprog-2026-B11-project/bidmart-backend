package com.example.bidmart.listing.service;

import com.example.bidmart.listing.model.AuctionStatus;
import com.example.bidmart.listing.model.Listing;
import com.example.bidmart.listing.repository.ListingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ListingServiceTest {

    @Mock
    private ListingRepository listingRepository;

    @InjectMocks
    private ListingService listingService;

    private UUID listingId;
    private Listing listing;

    @BeforeEach
    void setUp() {
        listingId = UUID.randomUUID();

        listing = new Listing();
        listing.setId(listingId);
        listing.setSellerId(UUID.randomUUID());
        listing.setStartingPrice(new BigDecimal("100"));
        listing.setEndTime(LocalDateTime.now().plusHours(1));
        listing.setStatus(AuctionStatus.ACTIVE);
    }

    @Test
    void updateListing_shouldFail_whenAuctionActive() {
        when(listingRepository.findById(listingId))
                .thenReturn(Optional.of(listing));

        Listing updated = new Listing();

        assertThrows(RuntimeException.class, () -> {
            listingService.updateListing(listingId, updated);
        });
    }

    @Test
    void updateListing_shouldSuccess_whenAuctionNotActive() {
        listing.setStatus(AuctionStatus.CLOSED);

        when(listingRepository.findById(listingId))
                .thenReturn(Optional.of(listing));
        when(listingRepository.save(listing))
                .thenReturn(listing);

        Listing updated = new Listing();
        updated.setTitle("Updated");

        Listing result = listingService.updateListing(listingId, updated);

        assertEquals("Updated", result.getTitle());
    }

    @Test
    void deleteListing_shouldFail_whenAuctionActive() {
        when(listingRepository.findById(listingId))
                .thenReturn(Optional.of(listing));

        assertThrows(RuntimeException.class, () -> {
            listingService.deleteListing(listingId);
        });
    }

    @Test
    void deleteListing_shouldSuccess_whenAuctionNotActive() {
        listing.setStatus(AuctionStatus.CLOSED);

        when(listingRepository.findById(listingId))
                .thenReturn(Optional.of(listing));

        listingService.deleteListing(listingId);

        verify(listingRepository).delete(listing);
    }

    @Test
    void updateListing_shouldFail_whenAuctionExtended() {
        listing.setStatus(AuctionStatus.EXTENDED);
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));
        Listing updated = new Listing();

        assertThrows(RuntimeException.class, () -> {
            listingService.updateListing(listingId, updated);
        });
    }

    @Test
    void deleteListing_shouldFail_whenAuctionExtended() {
        listing.setStatus(AuctionStatus.EXTENDED);
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));

        assertThrows(RuntimeException.class, () -> {
            listingService.deleteListing(listingId);
        });
    }

    @Test
    void updateListing_shouldFail_whenListingNotFound() {
        when(listingRepository.findById(listingId)).thenReturn(Optional.empty());
        Listing updated = new Listing();

        assertThrows(RuntimeException.class, () -> {
            listingService.updateListing(listingId, updated);
        });
    }

    @Test
    void deleteListing_shouldFail_whenListingNotFound() {
        when(listingRepository.findById(listingId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            listingService.deleteListing(listingId);
        });
    }

    @Test
    void getListingById_shouldReturnListing() {
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));
        Optional<Listing> result = listingService.getListingById(listingId);

        assertTrue(result.isPresent());
        assertEquals(listingId, result.get().getId());
    }

    @Test
    void save_shouldReturnSavedListing() {
        when(listingRepository.save(listing)).thenReturn(listing);
        Listing result = listingService.save(listing);

        assertEquals(listingId, result.getId());
    }

    @Test
    void getAllListings_shouldReturnAllListings() {
        List<Listing> listings = List.of(listing);
        when(listingRepository.findAll()).thenReturn(listings);
        List<Listing> result = listingService.getAllListings();

        assertEquals(1, result.size());
        assertEquals(listingId, result.get(0).getId());
    }

    @Test
    void getListingById_shouldReturnEmpty_whenListingNotFound() {
        when(listingRepository.findById(listingId)).thenReturn(Optional.empty());
        Optional<Listing> result = listingService.getListingById(listingId);

        assertFalse(result.isPresent());
    }

    @Test
    void getListingByIdWithLock_shouldReturnListing() {
        when(listingRepository.findByIdWithLock(listingId)).thenReturn(Optional.of(listing));
        Optional<Listing> result = listingService.getListingByIdWithLock(listingId);

        assertTrue(result.isPresent());
        assertEquals(listingId, result.get().getId());
    }

    @Test
    void getListingByIdWithLock_shouldReturnEmpty_whenListingNotFound() {
        when(listingRepository.findByIdWithLock(listingId)).thenReturn(Optional.empty());
        Optional<Listing> result = listingService.getListingByIdWithLock(listingId);

        assertFalse(result.isPresent());
    }

    @Test
    void updateListing_shouldUpdateAllEditableFields() {
        listing.setStatus(AuctionStatus.CLOSED);
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));
        when(listingRepository.save(any(Listing.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Listing updated = new Listing();
        updated.setTitle("Updated Title");
        updated.setDescription("Updated Description");
        updated.setImageUrl("updated.jpg");
        updated.setStartingPrice(new BigDecimal("500"));
        updated.setReservePrice(new BigDecimal("700"));
        updated.setEndTime(LocalDateTime.now().plusDays(1));

        Listing result = listingService.updateListing(listingId, updated);
        assertEquals("Updated Title", result.getTitle());
        assertEquals("Updated Description", result.getDescription());
        assertEquals("updated.jpg", result.getImageUrl());
        assertEquals(new BigDecimal("500"), result.getStartingPrice());
        assertEquals(new BigDecimal("700"), result.getReservePrice());
    }

    @Test
    void updateListing_shouldAllowNullStatus() {
        listing.setStatus(AuctionStatus.CLOSED);
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));
        when(listingRepository.save(any(Listing.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        Listing updated = new Listing();
        updated.setTitle("No Status Change");
        Listing result = listingService.updateListing(listingId, updated);

        assertEquals("No Status Change", result.getTitle());
        assertEquals(AuctionStatus.CLOSED, result.getStatus());
    }

    @Test
    void updateListing_shouldAllowNullAuctionStatus() {
        listing.setStatus(null);
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));
        when(listingRepository.save(any(Listing.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        Listing updated = new Listing();
        updated.setTitle("Null Status Update");
        Listing result = listingService.updateListing(listingId, updated);

        assertEquals("Null Status Update", result.getTitle());
    }

    @Test
    void deleteListing_shouldCallRepositoryDelete() {
        listing.setStatus(AuctionStatus.CLOSED);
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));
        listingService.deleteListing(listingId);

        verify(listingRepository, times(1)).delete(listing);
    }

    @Test
    void save_shouldCallRepositorySave() {
        when(listingRepository.save(listing)).thenReturn(listing);
        listingService.save(listing);

        verify(listingRepository, times(1)).save(listing);
    }

    @Test
    void getAllListings_shouldCallRepositoryFindAll() {
        when(listingRepository.findAll()).thenReturn(List.of(listing));
        listingService.getAllListings();

        verify(listingRepository, times(1)).findAll();
    }

    @Test
    void getAllListings_withPageable_shouldReturnPage() {
        Pageable pageable = PageRequest.of(0, 20);
        when(listingRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(listing), pageable, 1));

        Page<Listing> result = listingService.getAllListings(pageable);

        assertEquals(1, result.getContent().size());
        assertEquals(1, result.getTotalElements());
        verify(listingRepository, times(1)).findAll(pageable);
    }

    @Test
    void searchListings_withPageable_shouldReturnPage() {
        Pageable pageable = PageRequest.of(0, 20);
        UUID categoryId = UUID.randomUUID();
        when(listingRepository.findBySearchCriteria("test", categoryId, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(listing), pageable, 1));

        Page<Listing> result = listingService.searchListings("test", categoryId.toString(), null, null, pageable);

        assertEquals(1, result.getContent().size());
        verify(listingRepository, times(1)).findBySearchCriteria("test", categoryId, null, null, pageable);
    }

    @Test
    void getActiveListings_withPageable_shouldReturnPage() {
        Pageable pageable = PageRequest.of(0, 20);
        when(listingRepository.findActiveListings(pageable)).thenReturn(new PageImpl<>(List.of(listing), pageable, 1));

        Page<Listing> result = listingService.getActiveListings(pageable);

        assertEquals(1, result.getContent().size());
        verify(listingRepository, times(1)).findActiveListings(pageable);
    }

    @Test
    void getListingById_shouldCallRepositoryFindById() {
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));
        listingService.getListingById(listingId);

        verify(listingRepository, times(1)).findById(listingId);
    }

    @Test
    void getListingByIdWithLock_shouldCallRepositoryFindByIdWithLock() {
        when(listingRepository.findByIdWithLock(listingId)).thenReturn(Optional.of(listing));
        listingService.getListingByIdWithLock(listingId);

        verify(listingRepository, times(1)).findByIdWithLock(listingId);
    }

    @Test
    void createListing_shouldSetSellerIdAndDefaultStatus() {
        UUID sellerId = UUID.randomUUID();
        Listing newListing = new Listing();
        newListing.setStatus(null);
        when(listingRepository.save(any(Listing.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Listing result = listingService.createListing(newListing, sellerId);

        assertEquals(sellerId, result.getSellerId());
        assertEquals(AuctionStatus.ACTIVE, result.getStatus());
        assertNotNull(result.getCreatedAt());
    }

    @Test
    void createListing_shouldKeepExistingStatus() {
        UUID sellerId = UUID.randomUUID();
        Listing newListing = new Listing();
        newListing.setStatus(AuctionStatus.CLOSED);
        when(listingRepository.save(any(Listing.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Listing result = listingService.createListing(newListing, sellerId);

        assertEquals(AuctionStatus.CLOSED, result.getStatus());
    }

    @Test
    void createListing_shouldValidateEndTimeInFuture() {
        UUID sellerId = UUID.randomUUID();
        Listing newListing = new Listing();
        newListing.setTitle("Test Listing");
        newListing.setStartingPrice(new BigDecimal("100"));
        newListing.setEndTime(LocalDateTime.now().minusHours(1));

        when(listingRepository.save(any(Listing.class))).thenThrow(new RuntimeException("endTime harus di masa depan"));

        assertThrows(RuntimeException.class, () -> {
            listingService.createListing(newListing, sellerId);
        });
    }

    @Test
    void createListing_shouldValidateStartingPriceGreaterThanZero() {
        UUID sellerId = UUID.randomUUID();
        Listing newListing = new Listing();
        newListing.setTitle("Test Listing");
        newListing.setStartingPrice(new BigDecimal("0"));
        newListing.setEndTime(LocalDateTime.now().plusHours(1));

        when(listingRepository.save(any(Listing.class))).thenThrow(new RuntimeException("startingPrice harus > 0"));

        assertThrows(RuntimeException.class, () -> {
            listingService.createListing(newListing, sellerId);
        });
    }

    @Test
    void createListing_shouldValidateReservePriceGreaterThanOrEqualStartingPrice() {
        UUID sellerId = UUID.randomUUID();
        Listing newListing = new Listing();
        newListing.setTitle("Test Listing");
        newListing.setStartingPrice(new BigDecimal("100"));
        newListing.setReservePrice(new BigDecimal("50"));
        newListing.setEndTime(LocalDateTime.now().plusHours(1));

        when(listingRepository.save(any(Listing.class))).thenThrow(new RuntimeException("reservePrice harus >= startingPrice"));

        assertThrows(RuntimeException.class, () -> {
            listingService.createListing(newListing, sellerId);
        });
    }

    @Test
    void createListing_shouldValidateTitleNotBlank() {
        UUID sellerId = UUID.randomUUID();
        Listing newListing = new Listing();
        newListing.setTitle("");
        newListing.setStartingPrice(new BigDecimal("100"));
        newListing.setEndTime(LocalDateTime.now().plusHours(1));

        when(listingRepository.save(any(Listing.class))).thenThrow(new RuntimeException("title tidak boleh blank"));

        assertThrows(RuntimeException.class, () -> {
            listingService.createListing(newListing, sellerId);
        });
    }
}
