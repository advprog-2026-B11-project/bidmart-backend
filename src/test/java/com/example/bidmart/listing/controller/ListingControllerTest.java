package com.example.bidmart.listing.controller;

import com.example.bidmart.listing.dto.PaginatedResponse;
import com.example.bidmart.listing.model.AuctionStatus;
import com.example.bidmart.listing.model.Listing;
import com.example.bidmart.listing.service.ListingService;
import com.example.bidmart.user.service.UserService;
import org.springframework.security.core.Authentication;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class ListingControllerTest {

    @Mock
    private ListingService listingService;

    @Mock
    private UserService userService;

    @InjectMocks
    private ListingController listingController;

    private UUID listingId;
    private Listing listing;

    @BeforeEach
    void setUp() {
        listingId = UUID.randomUUID();

        listing = new Listing();
        listing.setId(listingId);
        listing.setTitle("Test");
        listing.setStartingPrice(new BigDecimal("100"));
        listing.setEndTime(LocalDateTime.now().plusHours(1));
        listing.setStatus(AuctionStatus.CLOSED);
    }

    @Test
    void updateListing_success() {
        when(listingService.updateListing(listingId, listing))
                .thenReturn(listing);

        ResponseEntity<?> response = listingController.updateListing(listingId, listing);

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
    }

    @Test
    void updateListing_fail_whenAuctionActive() {
        when(listingService.updateListing(listingId, listing))
                .thenThrow(new RuntimeException("Listing tidak bisa diupdate saat auction masih aktif."));

        ResponseEntity<?> response = listingController.updateListing(listingId, listing);

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Listing tidak bisa diupdate saat auction masih aktif.", response.getBody());
    }

    @Test
    void deleteListing_success() {
        ResponseEntity<?> response = listingController.deleteListing(listingId);

        assertEquals(204, response.getStatusCode().value());
    }

    @Test
    void deleteListing_fail_whenAuctionActive() {
        doThrow(new RuntimeException("Listing tidak bisa dihapus saat auction masih aktif."))
                .when(listingService).deleteListing(listingId);

        ResponseEntity<?> response = listingController.deleteListing(listingId);

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Listing tidak bisa dihapus saat auction masih aktif.", response.getBody());
    }

    @Test
    void getAllListings_shouldReturnOk() {
        when(listingService.getAllListings(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(listing)));

        ResponseEntity<PaginatedResponse<Listing>> response = listingController.getAllListings(0, 20);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().content().size());
        assertEquals(0, response.getBody().page());
        assertTrue(response.getBody().last());
    }

    @Test
    void getListingById_shouldReturnListing() {
        when(listingService.getListingById(listingId))
                .thenReturn(Optional.of(listing));

        ResponseEntity<Listing> response = listingController.getListingById(listingId);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(listingId, response.getBody().getId());
    }

    @Test
    void getListingById_shouldReturnNotFound() {
        when(listingService.getListingById(listingId))
                .thenReturn(Optional.empty());

        ResponseEntity<Listing> response = listingController.getListingById(listingId);

        assertEquals(404, response.getStatusCode().value());
    }

    @Test
    void createListing_shouldReturnCreatedListing() {
        Authentication authentication = mock(Authentication.class);
        UUID sellerId = UUID.randomUUID();

        when(authentication.getName()).thenReturn("karla");
        when(userService.getUserIdByUsername("karla")).thenReturn(sellerId);
        when(listingService.createListing(listing, sellerId)).thenReturn(listing);

        ResponseEntity<Listing> response =
                listingController.createListing(listing, authentication);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(listingId, response.getBody().getId());
    }

    @Test
    void createListing_shouldFail_whenAuthenticationNull() {
        assertThrows(Exception.class, () -> {
            listingController.createListing(listing, null);
        });
    }

    @Test
    void searchListings_shouldReturnListings() {
        when(listingService.searchListings(eq("test"), eq(null), eq(null), eq(null), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(listing)));

        ResponseEntity<PaginatedResponse<Listing>> response = listingController.searchListings("test", null, null, null, 0, 20);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().content().size());
    }

    @Test
    void searchListings_shouldReturnEmptyList() {
        when(listingService.searchListings(eq("nonexistent"), eq(null), eq(null), eq(null), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        ResponseEntity<PaginatedResponse<Listing>> response = listingController.searchListings("nonexistent", null, null, null, 0, 20);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(0, response.getBody().content().size());
    }

    @Test
    void getActiveListings_shouldReturnOnlyActiveListings() {
        Listing activeListing = new Listing();
        activeListing.setId(UUID.randomUUID());
        activeListing.setTitle("Active Auction");
        activeListing.setStartingPrice(new BigDecimal("100"));
        activeListing.setEndTime(LocalDateTime.now().plusHours(2));
        activeListing.setStatus(AuctionStatus.ACTIVE);

        when(listingService.getActiveListings(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(activeListing)));

        ResponseEntity<PaginatedResponse<Listing>> response = listingController.getActiveListings(0, 20);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().content().size());
        assertEquals(AuctionStatus.ACTIVE, response.getBody().content().get(0).getStatus());
    }

    @Test
    void getActiveListings_shouldReturnEmptyWhenNoActiveListings() {
        when(listingService.getActiveListings(any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        ResponseEntity<PaginatedResponse<Listing>> response = listingController.getActiveListings(0, 20);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(0, response.getBody().content().size());
    }
}
