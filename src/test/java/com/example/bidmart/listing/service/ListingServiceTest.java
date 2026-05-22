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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.security.access.AccessDeniedException;
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
        sellerId = UUID.randomUUID();
        listing = new Listing();
        listing.setId(listingId);
        listing.setSellerId(sellerId);
        listing.setStartingPrice(new BigDecimal("100"));
        listing.setEndTime(LocalDateTime.now().plusHours(1));
        listing.setStatus(AuctionStatus.ACTIVE);
    }

    @Test
    void createListing_shouldSetSellerIdStatusAuctionTypeAndFields() {
        UUID categoryId = UUID.randomUUID();
        CreateListingRequest request = request(categoryId, "New Listing", new BigDecimal("100"), null, AuctionType.ENGLISH);
        when(listingRepository.save(any(Listing.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Listing result = listingService.createListing(request, sellerId);

        assertEquals(sellerId, result.getSellerId());
        assertEquals(categoryId, result.getCategoryId());
        assertEquals("New Listing", result.getTitle());
        assertEquals(AuctionStatus.ACTIVE, result.getStatus());
        assertEquals(AuctionType.ENGLISH, result.getAuctionType());
        assertNotNull(result.getCreatedAt());
    }

    @Test
    void createListing_shouldDefaultAuctionTypeToEnglish() {
        CreateListingRequest request = request(UUID.randomUUID(), "New Listing", new BigDecimal("100"), null, null);
        when(listingRepository.save(any(Listing.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Listing result = listingService.createListing(request, sellerId);

        assertEquals(AuctionType.ENGLISH, result.getAuctionType());
    }

    @Test
    void createListing_shouldFail_whenReservePriceLowerThanStartingPrice() {
        CreateListingRequest request = request(UUID.randomUUID(), "New Listing", new BigDecimal("100"), new BigDecimal("50"), null);

        assertThrows(IllegalArgumentException.class, () -> listingService.createListing(request, sellerId));
    }

    @Test
    void getAllListings_shouldReturnAllListings() {
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
    void getListingById_shouldReturnListing() {
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));

        Optional<Listing> result = listingService.getListingById(listingId);

        assertTrue(result.isPresent());
        assertEquals(listingId, result.get().getId());
    }

    @Test
    void getListingByIdWithLock_shouldReturnListing() {
        when(listingRepository.findByIdWithLock(listingId)).thenReturn(Optional.of(listing));

        Optional<Listing> result = listingService.getListingByIdWithLock(listingId);

        assertTrue(result.isPresent());
        verify(listingRepository).findByIdWithLock(listingId);
    }

    @Test
    void save_shouldDelegateToRepository() {
        when(listingRepository.save(listing)).thenReturn(listing);

        Listing result = listingService.save(listing);

        assertEquals(listingId, result.getId());
        verify(listingRepository).save(listing);
    }

    @Test
    void updateListing_shouldFail_whenNotFound() {
        when(listingRepository.findById(listingId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> listingService.updateListing(listingId, validRequest(), sellerId, false));
    }

    @Test
    void updateListing_shouldFail_whenNotOwnerAndNotAdmin() {
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));

        assertThrows(AccessDeniedException.class, () -> listingService.updateListing(listingId, validRequest(), UUID.randomUUID(), false));
    }

    @Test
    void updateListing_shouldFail_whenAuctionActive() {
        listing.setStatus(AuctionStatus.ACTIVE);
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));

        assertThrows(IllegalArgumentException.class, () -> listingService.updateListing(listingId, validRequest(), sellerId, false));
    }

    @Test
    void updateListing_shouldFail_whenAuctionExtended() {
        listing.setStatus(AuctionStatus.EXTENDED);
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));

        assertThrows(IllegalArgumentException.class, () -> listingService.updateListing(listingId, validRequest(), sellerId, false));
    }

    @Test
    void updateListing_shouldUpdateAllEditableFields_whenNotActive() {
        listing.setStatus(AuctionStatus.CLOSED);
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));
        when(listingRepository.save(any(Listing.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Listing result = listingService.updateListing(listingId, validRequest(), sellerId, false);

        assertEquals("Updated Title", result.getTitle());
        assertEquals("desc", result.getDescription());
        assertEquals("img.jpg", result.getImageUrl());
        assertEquals(new BigDecimal("500"), result.getStartingPrice());
        assertEquals(new BigDecimal("700"), result.getReservePrice());
        assertEquals(AuctionType.ENGLISH, result.getAuctionType());
    }

    @Test
    void updateListing_shouldDefaultAuctionTypeToEnglish_whenNull() {
        listing.setStatus(AuctionStatus.CLOSED);
        CreateListingRequest request = request(UUID.randomUUID(), "Title", new BigDecimal("100"), null, null);
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));
        when(listingRepository.save(any(Listing.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Listing result = listingService.updateListing(listingId, request, sellerId, false);
        assertEquals(AuctionType.ENGLISH, result.getAuctionType());
    }

    @Test
    void updateListing_shouldFail_whenReservePriceLowerThanStartingPrice() {
        listing.setStatus(AuctionStatus.CLOSED);
        CreateListingRequest request = request(UUID.randomUUID(), "Title", new BigDecimal("100"), new BigDecimal("50"), AuctionType.ENGLISH);
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));

        assertThrows(IllegalArgumentException.class, () -> listingService.updateListing(listingId, request, sellerId, false));
    }

    @Test
    void updateListing_nullStatus_doesNotBlockUpdate() {
        listing.setStatus(null);
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));
        when(listingRepository.save(any(Listing.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Listing result = listingService.updateListing(listingId, validRequest(), sellerId, false);
        assertEquals("Updated Title", result.getTitle());
    }

    @Test
    void deleteListing_shouldFail_whenNotFound() {
        when(listingRepository.findById(listingId)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> listingService.deleteListing(listingId, sellerId, false));
    }

    @Test
    void updateListing_shouldSucceedForAdminEvenWhenNotOwner() {
        listing.setStatus(AuctionStatus.CLOSED);
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));
        when(listingRepository.save(any(Listing.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Listing result = listingService.updateListing(listingId, validRequest(), UUID.randomUUID(), true);

        assertEquals("Updated Title", result.getTitle());
    }

    @Test
    void deleteListing_shouldFail_whenNotOwnerAndNotAdmin() {
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));

        assertThrows(AccessDeniedException.class, () -> listingService.deleteListing(listingId, UUID.randomUUID(), false));
    }

    @Test
    void deleteListing_shouldFail_whenAuctionActive() {
        listing.setStatus(AuctionStatus.ACTIVE);
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));

        assertThrows(IllegalArgumentException.class, () -> listingService.deleteListing(listingId, sellerId, false));
    }

    @Test
    void deleteListing_shouldDelete_whenNotActive() {
        listing.setStatus(AuctionStatus.CLOSED);
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));

        listingService.deleteListing(listingId, sellerId, false);

        verify(listingRepository).delete(listing);
    }

    @Test
    void deleteListing_shouldDeleteForAdminEvenWhenNotOwner() {
        listing.setStatus(AuctionStatus.CLOSED);
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));

        listingService.deleteListing(listingId, UUID.randomUUID(), true);

        verify(listingRepository).delete(listing);
    }

    @Test
    void searchListings_shouldValidateInvalidPriceRange() {
        assertThrows(IllegalArgumentException.class,
                () -> listingService.searchListings(null, null, new BigDecimal("200000"), new BigDecimal("100000")));
    }

    @Test
    void searchListings_shouldFail_whenMinPriceNegative() {
        assertThrows(IllegalArgumentException.class,
                () -> listingService.searchListings(null, null, new BigDecimal("-10"), new BigDecimal("100")));
    }

    @Test
    void searchListings_shouldFail_whenMaxPriceNegative() {
        assertThrows(IllegalArgumentException.class,
                () -> listingService.searchListings(null, null, null, new BigDecimal("-100")));
    }

    @Test
    void searchListings_withBlankKeyword_shouldNormalizeToNull() {
        when(listingRepository.findBySearchCriteria(null, null, null, null)).thenReturn(List.of(listing));

        List<Listing> result = listingService.searchListings("   ", null, null, null);

        assertEquals(1, result.size());
        verify(listingRepository).findBySearchCriteria(null, null, null, null);
    }

    @Test
    void searchListings_withNullKeyword_shouldPassNull() {
        when(listingRepository.findBySearchCriteria(null, null, null, null)).thenReturn(List.of(listing));

        List<Listing> result = listingService.searchListings(null, null, null, null);

        assertEquals(1, result.size());
        verify(listingRepository).findBySearchCriteria(null, null, null, null);
    }

    @Test
    void searchListings_withInvalidCategoryId_shouldParseAsNull() {
        when(listingRepository.findBySearchCriteria("test", null, null, null)).thenReturn(List.of(listing));

        List<Listing> result = listingService.searchListings("test", "not-a-uuid", null, null);

        assertEquals(1, result.size());
        verify(listingRepository).findBySearchCriteria("test", null, null, null);
    }

    @Test
    void searchListings_withEmptyCategory_shouldParseAsNull() {
        when(listingRepository.findBySearchCriteria("test", null, null, null)).thenReturn(List.of(listing));

        List<Listing> result = listingService.searchListings("test", "", null, null);

        assertEquals(1, result.size());
        verify(listingRepository).findBySearchCriteria("test", null, null, null);
    }

    @Test
    void searchListings_withNullCategory_shouldParseAsNull() {
        when(listingRepository.findBySearchCriteria("test", null, null, null)).thenReturn(List.of(listing));

        List<Listing> result = listingService.searchListings("test", null, null, null);

        assertEquals(1, result.size());
        verify(listingRepository).findBySearchCriteria("test", null, null, null);
    }

    @Test
    void searchListings_validPriceRange_shouldSucceed() {
        when(listingRepository.findBySearchCriteria(null, null, new BigDecimal("100"), new BigDecimal("500"))).thenReturn(List.of(listing));

        List<Listing> result = listingService.searchListings(null, null, new BigDecimal("100"), new BigDecimal("500"));

        assertEquals(1, result.size());
    }

    @Test
    void createListing_reservePriceNull_shouldSucceed() {
        CreateListingRequest request = request(UUID.randomUUID(), "Listing", new BigDecimal("100"), null, AuctionType.ENGLISH);
        when(listingRepository.save(any(Listing.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Listing result = listingService.createListing(request, sellerId);
        assertNotNull(result);
    }

    @Test
    void createListing_startingPriceNull_shouldSucceed() {
        CreateListingRequest request = request(UUID.randomUUID(), "Listing", null, null, AuctionType.ENGLISH);
        when(listingRepository.save(any(Listing.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Listing result = listingService.createListing(request, sellerId);
        assertNotNull(result);
    }

    @Test
    void searchListings_noPageable_shouldNormalizeKeywordAndCategory() {
        UUID categoryId = UUID.randomUUID();
        when(listingRepository.findBySearchCriteria("test", categoryId, null, null)).thenReturn(List.of(listing));

        List<Listing> result = listingService.searchListings(" test ", categoryId.toString(), null, null);

        assertEquals(1, result.size());
        verify(listingRepository).findBySearchCriteria("test", categoryId, null, null);
    }

    @Test
    void searchListings_withPageable_shouldStripSort() {
        Pageable sorted = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        Pageable unsorted = PageRequest.of(0, 20);
        when(listingRepository.findBySearchCriteria("test", null, null, null, unsorted))
                .thenReturn(new PageImpl<>(List.of(listing), unsorted, 1));

        Page<Listing> result = listingService.searchListings("test", null, null, null, sorted);

        assertEquals(1, result.getContent().size());
        verify(listingRepository).findBySearchCriteria("test", null, null, null, unsorted);
    }

    @Test
    void getActiveListings_shouldReturnActiveListings() {
        when(listingRepository.findActiveListings()).thenReturn(List.of(listing));

        List<Listing> result = listingService.getActiveListings();

        assertEquals(1, result.size());
    }

    @Test
    void getActiveListings_withPageable_shouldReturnPage() {
        Pageable pageable = PageRequest.of(0, 20);
        when(listingRepository.findActiveListings(pageable)).thenReturn(new PageImpl<>(List.of(listing), pageable, 1));

        Page<Listing> result = listingService.getActiveListings(pageable);

        assertEquals(1, result.getContent().size());
    }

    private CreateListingRequest validRequest() {
        return request(UUID.randomUUID(), "Updated Title", new BigDecimal("500"), new BigDecimal("700"), AuctionType.ENGLISH);
    }

    private CreateListingRequest request(UUID categoryId, String title, BigDecimal startingPrice,
            BigDecimal reservePrice, AuctionType auctionType) {
        return new CreateListingRequest(categoryId, title, "desc", "img.jpg",
                startingPrice, reservePrice, LocalDateTime.now().plusDays(1), auctionType);
    }
}