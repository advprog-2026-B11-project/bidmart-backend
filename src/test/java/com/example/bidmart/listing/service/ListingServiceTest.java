package com.example.bidmart.listing.service;

import com.example.bidmart.listing.dto.CreateListingRequest;
import com.example.bidmart.listing.model.AuctionType;
import com.example.bidmart.listing.model.AuctionStatus;
import com.example.bidmart.listing.model.Listing;
import com.example.bidmart.listing.repository.ListingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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
    void createListing_shouldSetSellerIdDefaultStatusAndRequestFields() {
        UUID sellerId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        CreateListingRequest request = createRequest(categoryId, "Test Listing", new BigDecimal("100"),
                new BigDecimal("150"), LocalDateTime.now().plusHours(1), AuctionType.ENGLISH);

        when(listingRepository.save(any(Listing.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Listing result = listingService.createListing(request, sellerId);

        assertEquals(sellerId, result.getSellerId());
        assertEquals(categoryId, result.getCategoryId());
        assertEquals("Test Listing", result.getTitle());
        assertEquals(AuctionStatus.ACTIVE, result.getStatus());
        assertEquals(AuctionType.ENGLISH, result.getAuctionType());
        assertNotNull(result.getCreatedAt());
    }

    @Test
    void createListing_shouldDefaultAuctionTypeToEnglishWhenRequestAuctionTypeNull() {
        UUID sellerId = UUID.randomUUID();
        CreateListingRequest request = createRequest(UUID.randomUUID(), "Test Listing", new BigDecimal("100"),
                null, LocalDateTime.now().plusHours(1), null);

        when(listingRepository.save(any(Listing.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Listing result = listingService.createListing(request, sellerId);

        assertEquals(AuctionType.ENGLISH, result.getAuctionType());
    }

    @Test
    void createListing_shouldValidateEndTimeInFuture() {
        UUID sellerId = UUID.randomUUID();
        CreateListingRequest request = createRequest(UUID.randomUUID(), "Test Listing",
                new BigDecimal("100"), null, LocalDateTime.now().minusHours(1), null);

        when(listingRepository.save(any(Listing.class))).thenThrow(new RuntimeException("endTime harus di masa depan"));

        assertThrows(RuntimeException.class, () -> listingService.createListing(request, sellerId));
    }

    @Test
    void createListing_shouldValidateStartingPriceGreaterThanZero() {
        UUID sellerId = UUID.randomUUID();
        CreateListingRequest request = createRequest(UUID.randomUUID(), "Test Listing",
                new BigDecimal("0"), null, LocalDateTime.now().plusHours(1), null);

        when(listingRepository.save(any(Listing.class))).thenThrow(new RuntimeException("startingPrice harus > 0"));

        assertThrows(RuntimeException.class, () -> listingService.createListing(request, sellerId));
    }

    @Test
    void createListing_shouldValidateReservePriceGreaterThanOrEqualStartingPrice() {
        UUID sellerId = UUID.randomUUID();
        CreateListingRequest request = createRequest(UUID.randomUUID(), "Test Listing",
                new BigDecimal("100"), new BigDecimal("50"), LocalDateTime.now().plusHours(1), null);

        assertThrows(IllegalArgumentException.class, () -> listingService.createListing(request, sellerId));
    }

    @Test
    void createListing_shouldValidateTitleNotBlank() {
        UUID sellerId = UUID.randomUUID();
        CreateListingRequest request = createRequest(UUID.randomUUID(), "",
                new BigDecimal("100"), null, LocalDateTime.now().plusHours(1), null);

        when(listingRepository.save(any(Listing.class))).thenThrow(new RuntimeException("title tidak boleh blank"));

        assertThrows(RuntimeException.class, () -> listingService.createListing(request, sellerId));
    }

    private CreateListingRequest createRequest(UUID categoryId, String title, BigDecimal startingPrice,
                                               BigDecimal reservePrice, LocalDateTime endTime, AuctionType auctionType) {
        return new CreateListingRequest(categoryId, title, "Description", "image.jpg",
                startingPrice, reservePrice, endTime, auctionType);
    }
}