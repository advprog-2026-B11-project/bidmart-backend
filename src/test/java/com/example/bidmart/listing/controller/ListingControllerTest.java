package com.example.bidmart.listing.controller;

import com.example.bidmart.listing.dto.CreateListingRequest;
import com.example.bidmart.listing.dto.ListingResponse;
import com.example.bidmart.listing.dto.PaginatedResponse;
import com.example.bidmart.listing.model.AuctionStatus;
import com.example.bidmart.listing.model.AuctionType;
import com.example.bidmart.listing.model.Listing;
import com.example.bidmart.listing.service.ListingService;
import com.example.bidmart.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ListingControllerTest {

    @Mock private ListingService listingService;
    @Mock private UserService userService;
    @InjectMocks private ListingController listingController;

    private UUID listingId;
    private UUID sellerId;
    private Listing listing;
    private CreateListingRequest request;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        listingId = UUID.randomUUID();
        sellerId  = UUID.randomUUID();

        listing = new Listing();
        listing.setId(listingId);
        listing.setSellerId(sellerId);
        listing.setCategoryId(UUID.randomUUID());
        listing.setTitle("Test");
        listing.setStartingPrice(new BigDecimal("100"));
        listing.setEndTime(LocalDateTime.now().plusHours(1));
        listing.setStatus(AuctionStatus.ACTIVE);
        listing.setAuctionType(AuctionType.ENGLISH);
        listing.setCreatedAt(LocalDateTime.now());

        request = new CreateListingRequest(
                listing.getCategoryId(),
                "Test",
                null,
                null,
                new BigDecimal("100"),
                null,
                LocalDateTime.now().plusHours(1)
        );

        authentication = mock(Authentication.class);
        lenient().when(authentication.getName()).thenReturn("testuser");
        lenient().when(userService.getUserIdByUsername("testuser")).thenReturn(sellerId);
    }

    // ── CREATE ───────────────────────────────────────────────────────────────

    @Test
    void createListing_shouldReturn201() {
        when(listingService.createListing(any(CreateListingRequest.class), eq(sellerId))).thenReturn(listing);

        ResponseEntity<ListingResponse> response = listingController.createListing(request, authentication);

        assertEquals(HttpStatus.CREATED.value(), response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(listingId, response.getBody().getId());
    }

    @Test
    void createListing_shouldFail_whenAuthenticationNull() {
        assertThrows(Exception.class, () -> listingController.createListing(request, null));
    }

    // ── GET ALL ──────────────────────────────────────────────────────────────

    @Test
    void getAllListings_shouldReturn200WithContent() {
        when(listingService.getAllListings(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(listing)));

        ResponseEntity<PaginatedResponse<ListingResponse>> response = listingController.getAllListings(0, 20);

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().content().size());
        assertEquals(listingId, response.getBody().content().get(0).getId());
    }

    @Test
    void getAllListings_shouldReturn200WhenEmpty() {
        when(listingService.getAllListings(any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        ResponseEntity<PaginatedResponse<ListingResponse>> response = listingController.getAllListings(0, 20);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(0, response.getBody().content().size());
    }

    // ── GET ACTIVE ───────────────────────────────────────────────────────────

    @Test
    void getActiveListings_shouldReturnOnlyActiveListings() {
        Listing active = new Listing();
        active.setId(UUID.randomUUID());
        active.setStatus(AuctionStatus.ACTIVE);
        active.setAuctionType(AuctionType.ENGLISH);
        active.setStartingPrice(new BigDecimal("100"));
        active.setEndTime(LocalDateTime.now().plusHours(2));
        active.setCreatedAt(LocalDateTime.now());

        when(listingService.getActiveListings(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(active)));

        ResponseEntity<PaginatedResponse<ListingResponse>> response = listingController.getActiveListings(0, 20);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().content().size());
        assertEquals(AuctionStatus.ACTIVE, response.getBody().content().get(0).getStatus());
    }

    @Test
    void getActiveListings_shouldReturn200WhenEmpty() {
        when(listingService.getActiveListings(any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        ResponseEntity<PaginatedResponse<ListingResponse>> response = listingController.getActiveListings(0, 20);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(0, response.getBody().content().size());
    }

    // ── GET BY ID ────────────────────────────────────────────────────────────

    @Test
    void getListingById_shouldReturn200() {
        when(listingService.getListingById(listingId)).thenReturn(Optional.of(listing));

        ResponseEntity<ListingResponse> response = listingController.getListingById(listingId);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(listingId, response.getBody().getId());
        assertEquals("Test", response.getBody().getTitle());
    }

    @Test
    void getListingById_shouldReturn404WhenNotFound() {
        when(listingService.getListingById(listingId)).thenReturn(Optional.empty());

        ResponseEntity<ListingResponse> response = listingController.getListingById(listingId);

        assertEquals(404, response.getStatusCode().value());
    }

    // ── SEARCH ───────────────────────────────────────────────────────────────

    @Test
    void searchListings_shouldReturn200WithResults() {
        when(listingService.searchListings(eq("test"), eq(null), eq(null), eq(null), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(listing)));

        ResponseEntity<PaginatedResponse<ListingResponse>> response =
                listingController.searchListings("test", null, null, null, 0, 20);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().content().size());
    }

    @Test
    void searchListings_shouldReturn200WhenEmpty() {
        when(listingService.searchListings(eq("xyz"), eq(null), eq(null), eq(null), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        ResponseEntity<PaginatedResponse<ListingResponse>> response =
                listingController.searchListings("xyz", null, null, null, 0, 20);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(0, response.getBody().content().size());
    }

    // ── UPDATE ───────────────────────────────────────────────────────────────

    @Test
    void updateListing_shouldReturn200OnSuccess() {
        listing.setStatus(AuctionStatus.CLOSED);
        when(listingService.updateListing(eq(listingId), any(CreateListingRequest.class), eq(sellerId)))
                .thenReturn(listing);

        ResponseEntity<?> response = listingController.updateListing(listingId, request, authentication);

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
    }

    @Test
    void updateListing_shouldReturn400WhenAuctionActive() {
        when(listingService.updateListing(eq(listingId), any(CreateListingRequest.class), eq(sellerId)))
                .thenThrow(new RuntimeException("Listing tidak bisa diupdate saat auction masih aktif."));

        ResponseEntity<?> response = listingController.updateListing(listingId, request, authentication);

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Listing tidak bisa diupdate saat auction masih aktif.", response.getBody());
    }

    @Test
    void updateListing_shouldReturn400WhenNotOwner() {
        when(listingService.updateListing(eq(listingId), any(CreateListingRequest.class), eq(sellerId)))
                .thenThrow(new RuntimeException("Hanya seller pemilik listing yang dapat mengubah data ini."));

        ResponseEntity<?> response = listingController.updateListing(listingId, request, authentication);

        assertEquals(400, response.getStatusCode().value());
    }

    // ── DELETE ───────────────────────────────────────────────────────────────

    @Test
    void deleteListing_shouldReturn204OnSuccess() {
        doNothing().when(listingService).deleteListing(eq(listingId), eq(sellerId));

        ResponseEntity<?> response = listingController.deleteListing(listingId, authentication);

        assertEquals(204, response.getStatusCode().value());
        verify(listingService).deleteListing(listingId, sellerId);
    }

    @Test
    void deleteListing_shouldReturn400WhenAuctionActive() {
        doThrow(new RuntimeException("Listing tidak bisa dihapus saat auction masih aktif."))
                .when(listingService).deleteListing(eq(listingId), eq(sellerId));

        ResponseEntity<?> response = listingController.deleteListing(listingId, authentication);

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Listing tidak bisa dihapus saat auction masih aktif.", response.getBody());
    }

    @Test
    void deleteListing_shouldReturn400WhenNotOwner() {
        doThrow(new RuntimeException("Hanya seller pemilik listing yang dapat menghapus data ini."))
                .when(listingService).deleteListing(eq(listingId), eq(sellerId));

        ResponseEntity<?> response = listingController.deleteListing(listingId, authentication);

        assertEquals(400, response.getStatusCode().value());
    }
}
