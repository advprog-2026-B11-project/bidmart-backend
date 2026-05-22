package com.example.bidmart.listing.service;

import com.example.bidmart.listing.dto.CreateListingRequest;
import com.example.bidmart.listing.model.AuctionStatus;
import com.example.bidmart.listing.model.AuctionType;
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
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ListingServiceTest {

    @Mock private ListingRepository listingRepository;
    @InjectMocks private ListingService listingService;

    private UUID listingId;
    private UUID sellerId;
    private Listing listing;

    @BeforeEach
    void setUp() {
        listingId = UUID.randomUUID();
        sellerId  = UUID.randomUUID();

        listing = new Listing();
        listing.setId(listingId);
        listing.setSellerId(sellerId);
        listing.setStartingPrice(new BigDecimal("100"));
        listing.setEndTime(LocalDateTime.now().plusHours(1));
        listing.setStatus(AuctionStatus.ACTIVE);
    }

    private CreateListingRequest validRequest() {
        return new CreateListingRequest(
                UUID.randomUUID(), "Updated Title", "desc", "img.jpg",
                new BigDecimal("500"), new BigDecimal("700"),
                LocalDateTime.now().plusDays(1)
        );
    }

    // ── CREATE ───────────────────────────────────────────────────────────────

    @Test
    void createListing_shouldSetSellerIdStatusAndCreatedAt() {
        when(listingRepository.save(any(Listing.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateListingRequest req = new CreateListingRequest(
                UUID.randomUUID(), "New Listing", null, null,
                new BigDecimal("100"), null, LocalDateTime.now().plusHours(2));

        Listing result = listingService.createListing(req, sellerId);

        assertEquals(sellerId, result.getSellerId());
        assertEquals(AuctionStatus.ACTIVE, result.getStatus());
        assertEquals(AuctionType.ENGLISH, result.getAuctionType());
        assertNotNull(result.getCreatedAt());
        assertEquals("New Listing", result.getTitle());
        verify(listingRepository).save(any(Listing.class));
    }

    @Test
    void createListing_shouldMapAllRequestFields() {
        UUID categoryId = UUID.randomUUID();
        LocalDateTime endTime = LocalDateTime.now().plusDays(3);
        CreateListingRequest req = new CreateListingRequest(
                categoryId, "Test", "desc", "img.jpg",
                new BigDecimal("200"), new BigDecimal("300"), endTime);

        when(listingRepository.save(any(Listing.class))).thenAnswer(inv -> inv.getArgument(0));

        Listing result = listingService.createListing(req, sellerId);

        assertEquals(categoryId, result.getCategoryId());
        assertEquals("Test", result.getTitle());
        assertEquals("desc", result.getDescription());
        assertEquals("img.jpg", result.getImageUrl());
        assertEquals(new BigDecimal("200"), result.getStartingPrice());
        assertEquals(new BigDecimal("300"), result.getReservePrice());
        assertEquals(endTime, result.getEndTime());
    }

    // ── GET ──────────────────────────────────────────────────────────────────

    @Test
    void getListingById_shouldReturnListing() {
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));

        Optional<Listing> result = listingService.getListingById(listingId);

        assertTrue(result.isPresent());
        assertEquals(listingId, result.get().getId());
    }

    @Test
    void getListingById_shouldReturnEmpty_whenNotFound() {
        when(listingRepository.findById(listingId)).thenReturn(Optional.empty());

        assertFalse(listingService.getListingById(listingId).isPresent());
    }

    @Test
    void getListingByIdWithLock_shouldReturnListing() {
        when(listingRepository.findByIdWithLock(listingId)).thenReturn(Optional.of(listing));

        assertTrue(listingService.getListingByIdWithLock(listingId).isPresent());
        verify(listingRepository).findByIdWithLock(listingId);
    }

    @Test
    void getListingByIdWithLock_shouldReturnEmpty_whenNotFound() {
        when(listingRepository.findByIdWithLock(listingId)).thenReturn(Optional.empty());

        assertFalse(listingService.getListingByIdWithLock(listingId).isPresent());
    }

    @Test
    void getAllListings_shouldReturnAll() {
        when(listingRepository.findAll()).thenReturn(List.of(listing));

        List<Listing> result = listingService.getAllListings();

        assertEquals(1, result.size());
        verify(listingRepository).findAll();
    }

    @Test
    void getAllListings_withPageable_shouldReturnPage() {
        Pageable pageable = PageRequest.of(0, 20);
        when(listingRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(listing), pageable, 1));

        Page<Listing> result = listingService.getAllListings(pageable);

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getActiveListings_withPageable_shouldReturnPage() {
        Pageable pageable = PageRequest.of(0, 20);
        when(listingRepository.findActiveListings(pageable)).thenReturn(new PageImpl<>(List.of(listing), pageable, 1));

        Page<Listing> result = listingService.getActiveListings(pageable);

        assertEquals(1, result.getContent().size());
        verify(listingRepository).findActiveListings(pageable);
    }

    @Test
    void searchListings_withPageable_stripsSort() {
        Pageable sortedPageable   = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        Pageable unsortedPageable = PageRequest.of(0, 20);
        UUID categoryId = UUID.randomUUID();

        when(listingRepository.findBySearchCriteria("test", categoryId, null, null, unsortedPageable))
                .thenReturn(new PageImpl<>(List.of(listing), unsortedPageable, 1));

        Page<Listing> result = listingService.searchListings("test", categoryId.toString(), null, null, sortedPageable);

        assertEquals(1, result.getContent().size());
        verify(listingRepository).findBySearchCriteria("test", categoryId, null, null, unsortedPageable);
    }

    @Test
    void save_shouldDelegateToRepository() {
        when(listingRepository.save(listing)).thenReturn(listing);

        Listing result = listingService.save(listing);

        assertEquals(listingId, result.getId());
        verify(listingRepository).save(listing);
    }

    // ── UPDATE ───────────────────────────────────────────────────────────────

    @Test
    void updateListing_shouldFail_whenNotFound() {
        when(listingRepository.findById(listingId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                listingService.updateListing(listingId, validRequest(), sellerId));
    }

    @Test
    void updateListing_shouldFail_whenNotOwner() {
        UUID otherId = UUID.randomUUID();
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                listingService.updateListing(listingId, validRequest(), otherId));

        assertTrue(ex.getMessage().contains("pemilik listing"));
    }

    @Test
    void updateListing_shouldFail_whenAuctionActive() {
        listing.setStatus(AuctionStatus.ACTIVE);
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));

        assertThrows(RuntimeException.class, () ->
                listingService.updateListing(listingId, validRequest(), sellerId));
    }

    @Test
    void updateListing_shouldFail_whenAuctionExtended() {
        listing.setStatus(AuctionStatus.EXTENDED);
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));

        assertThrows(RuntimeException.class, () ->
                listingService.updateListing(listingId, validRequest(), sellerId));
    }

    @Test
    void updateListing_shouldUpdateAllFields_whenNotActive() {
        listing.setStatus(AuctionStatus.CLOSED);
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));
        when(listingRepository.save(any(Listing.class))).thenAnswer(inv -> inv.getArgument(0));

        LocalDateTime newEnd = LocalDateTime.now().plusDays(1);
        CreateListingRequest req = new CreateListingRequest(
                UUID.randomUUID(), "New Title", "New Desc", "new.jpg",
                new BigDecimal("500"), new BigDecimal("700"), newEnd);

        Listing result = listingService.updateListing(listingId, req, sellerId);

        assertEquals("New Title", result.getTitle());
        assertEquals("New Desc", result.getDescription());
        assertEquals("new.jpg", result.getImageUrl());
        assertEquals(new BigDecimal("500"), result.getStartingPrice());
        assertEquals(new BigDecimal("700"), result.getReservePrice());
        assertEquals(newEnd, result.getEndTime());
    }

    @Test
    void updateListing_shouldSucceed_whenStatusUnsold() {
        listing.setStatus(AuctionStatus.UNSOLD);
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));
        when(listingRepository.save(any(Listing.class))).thenAnswer(inv -> inv.getArgument(0));

        Listing result = listingService.updateListing(listingId, validRequest(), sellerId);

        assertEquals("Updated Title", result.getTitle());
    }

    @Test
    void updateListing_shouldSucceed_whenStatusWon() {
        listing.setStatus(AuctionStatus.WON);
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));
        when(listingRepository.save(any(Listing.class))).thenAnswer(inv -> inv.getArgument(0));

        Listing result = listingService.updateListing(listingId, validRequest(), sellerId);

        assertNotNull(result);
    }

    // ── DELETE ───────────────────────────────────────────────────────────────

    @Test
    void deleteListing_shouldFail_whenNotFound() {
        when(listingRepository.findById(listingId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                listingService.deleteListing(listingId, sellerId));
    }

    @Test
    void deleteListing_shouldFail_whenNotOwner() {
        UUID otherId = UUID.randomUUID();
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                listingService.deleteListing(listingId, otherId));

        assertTrue(ex.getMessage().contains("pemilik listing"));
    }

    @Test
    void deleteListing_shouldFail_whenAuctionActive() {
        listing.setStatus(AuctionStatus.ACTIVE);
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));

        assertThrows(RuntimeException.class, () ->
                listingService.deleteListing(listingId, sellerId));
    }

    @Test
    void deleteListing_shouldFail_whenAuctionExtended() {
        listing.setStatus(AuctionStatus.EXTENDED);
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));

        assertThrows(RuntimeException.class, () ->
                listingService.deleteListing(listingId, sellerId));
    }

    @Test
    void deleteListing_shouldDelete_whenNotActive() {
        listing.setStatus(AuctionStatus.CLOSED);
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));

        listingService.deleteListing(listingId, sellerId);

        verify(listingRepository).delete(listing);
    }

    @Test
    void deleteListing_shouldDelete_whenStatusUnsold() {
        listing.setStatus(AuctionStatus.UNSOLD);
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));

        listingService.deleteListing(listingId, sellerId);

        verify(listingRepository, times(1)).delete(listing);
    }

    // ── STATUS NULL edge cases ────────────────────────────────────────────────

    @Test
    void updateListing_shouldSucceed_whenStatusNull() {
        listing.setStatus(null);
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));
        when(listingRepository.save(any(Listing.class))).thenAnswer(inv -> inv.getArgument(0));

        Listing result = listingService.updateListing(listingId, validRequest(), sellerId);

        assertEquals("Updated Title", result.getTitle());
    }

    @Test
    void deleteListing_shouldDelete_whenStatusNull() {
        listing.setStatus(null);
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));

        listingService.deleteListing(listingId, sellerId);

        verify(listingRepository).delete(listing);
    }

    // ── NON-PAGEABLE overloads ────────────────────────────────────────────────

    @Test
    void getActiveListings_noPageable_returnsAll() {
        when(listingRepository.findActiveListings()).thenReturn(List.of(listing));

        List<Listing> result = listingService.getActiveListings();

        assertEquals(1, result.size());
        verify(listingRepository).findActiveListings();
    }

    @Test
    void searchListings_noPageable_nullCategory_returnsList() {
        when(listingRepository.findBySearchCriteria("test", null, null, null))
                .thenReturn(List.of(listing));

        List<Listing> result = listingService.searchListings("test", null, null, null);

        assertEquals(1, result.size());
    }

    @Test
    void searchListings_noPageable_emptyCategory_treatedAsNull() {
        when(listingRepository.findBySearchCriteria("test", null, null, null))
                .thenReturn(List.of(listing));

        List<Listing> result = listingService.searchListings("test", "", null, null);

        assertEquals(1, result.size());
    }

    @Test
    void searchListings_noPageable_validCategoryUUID_passesUUID() {
        UUID categoryId = UUID.randomUUID();
        when(listingRepository.findBySearchCriteria(null, categoryId, null, null))
                .thenReturn(List.of(listing));

        List<Listing> result = listingService.searchListings(null, categoryId.toString(), null, null);

        assertEquals(1, result.size());
        verify(listingRepository).findBySearchCriteria(null, categoryId, null, null);
    }

    @Test
    void searchListings_noPageable_invalidCategoryUUID_treatedAsNull() {
        when(listingRepository.findBySearchCriteria(null, null, null, null))
                .thenReturn(List.of());

        List<Listing> result = listingService.searchListings(null, "not-a-uuid", null, null);

        assertTrue(result.isEmpty());
        verify(listingRepository).findBySearchCriteria(null, null, null, null);
    }

    @Test
    void searchListings_withPageable_invalidCategoryUUID_treatedAsNull() {
        Pageable pageable = PageRequest.of(0, 20);
        when(listingRepository.findBySearchCriteria(null, null, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of()));

        Page<Listing> result = listingService.searchListings(null, "bad-uuid", null, null, pageable);

        assertTrue(result.isEmpty());
    }

    @Test
    void searchListings_withPageable_emptyCategory_treatedAsNull() {
        Pageable pageable = PageRequest.of(0, 20);
        when(listingRepository.findBySearchCriteria(null, null, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(listing)));

        Page<Listing> result = listingService.searchListings(null, "", null, null, pageable);

        assertEquals(1, result.getContent().size());
    }
}
