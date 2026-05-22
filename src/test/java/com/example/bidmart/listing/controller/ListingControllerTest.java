package com.example.bidmart.listing.controller;

import com.example.bidmart.listing.dto.CreateListingRequest;
import com.example.bidmart.listing.dto.ListingResponse;
import com.example.bidmart.listing.dto.PaginatedResponse;
import com.example.bidmart.listing.model.AuctionStatus;
import com.example.bidmart.listing.model.AuctionType;
import com.example.bidmart.listing.model.Listing;
import com.example.bidmart.listing.service.ListingService;
import com.example.bidmart.user.service.UserService;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
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
        sellerId = UUID.randomUUID();

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

        request = new CreateListingRequest(listing.getCategoryId(), "Test", "desc", "img.jpg",
                new BigDecimal("100"), new BigDecimal("150"), LocalDateTime.now().plusHours(1), AuctionType.ENGLISH);

        authentication = mock(Authentication.class);
        lenient().when(authentication.getName()).thenReturn("testuser");
        lenient().doReturn(List.<GrantedAuthority>of()).when(authentication).getAuthorities();
        lenient().when(userService.getUserIdByUsername("testuser")).thenReturn(sellerId);
    }

    @Test
    void createListing_shouldReturn200() {
        when(listingService.createListing(any(CreateListingRequest.class), eq(sellerId))).thenReturn(listing);

        ResponseEntity<ListingResponse> response = listingController.createListing(request, authentication);

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(listingId, response.getBody().getId());
    }

    @Test
    void createListing_shouldFail_whenAuthenticationNull() {
        assertThrows(Exception.class, () -> listingController.createListing(request, null));
    }

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

    @Test
    void getActiveListings_shouldReturnOnlyActiveListings() {
        when(listingService.getActiveListings(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(listing)));

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

    @Test
    void searchListings_shouldReturn200WithResults() {
        when(listingService.searchListings(eq("test"), eq(null), eq(null), eq(null), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(listing)));

        ResponseEntity<?> response = listingController.searchListings("test", null, null, null, 0, 20);
        Object body = response.getBody();

        assertEquals(200, response.getStatusCode().value());
        assertTrue(body instanceof PaginatedResponse<?>);
        assertEquals(1, ((PaginatedResponse<?>) body).content().size());
    }

    @Test
    void searchListings_shouldReturn200WhenEmpty() {
        when(listingService.searchListings(eq("xyz"), eq(null), eq(null), eq(null), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        ResponseEntity<?> response = listingController.searchListings("xyz", null, null, null, 0, 20);
        Object body = response.getBody();

        assertEquals(200, response.getStatusCode().value());
        assertTrue(body instanceof PaginatedResponse<?>);
        assertEquals(0, ((PaginatedResponse<?>) body).content().size());
    }

    @Test
    void searchListings_shouldReturn400WhenPriceRangeInvalid() {
        when(listingService.searchListings(eq(null), eq(null), eq(new BigDecimal("200000")),
                eq(new BigDecimal("100000")), any(Pageable.class)))
                .thenThrow(new IllegalArgumentException("Minimum price tidak boleh lebih besar dari maximum price."));

        ResponseEntity<?> response = listingController.searchListings(null, null,
                new BigDecimal("200000"), new BigDecimal("100000"), 0, 20);

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Minimum price tidak boleh lebih besar dari maximum price.", response.getBody());
    }

    @Test
    void updateListing_shouldReturn200OnSuccess() {
        listing.setStatus(AuctionStatus.CLOSED);
        when(listingService.updateListing(eq(listingId), any(CreateListingRequest.class), eq(sellerId), eq(false)))
                .thenReturn(listing);

        ResponseEntity<?> response = listingController.updateListing(listingId, request, authentication);

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
    }

    @Test
    void updateListing_shouldReturn400WhenAuctionActive() {
        when(listingService.updateListing(eq(listingId), any(CreateListingRequest.class), eq(sellerId), eq(false)))
                .thenThrow(new IllegalArgumentException("Listing tidak bisa diupdate saat auction masih aktif."));

        ResponseEntity<?> response = listingController.updateListing(listingId, request, authentication);

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Listing tidak bisa diupdate saat auction masih aktif.", response.getBody());
    }

    @Test
    void updateListing_shouldReturn403WhenAccessDenied() {
        when(listingService.updateListing(eq(listingId), any(CreateListingRequest.class), eq(sellerId), eq(false)))
                .thenThrow(new AccessDeniedException("User tidak memiliki akses ke listing ini."));

        ResponseEntity<?> response = listingController.updateListing(listingId, request, authentication);

        assertEquals(403, response.getStatusCode().value());
    }

    @Test
    void deleteListing_shouldReturn204OnSuccess() {
        ResponseEntity<?> response = listingController.deleteListing(listingId, authentication);

        assertEquals(204, response.getStatusCode().value());
        verify(listingService).deleteListing(listingId, sellerId, false);
    }

    @Test
    void deleteListing_shouldReturn400WhenAuctionActive() {
        doThrow(new IllegalArgumentException("Listing tidak bisa dihapus saat auction masih aktif."))
                .when(listingService).deleteListing(listingId, sellerId, false);

        ResponseEntity<?> response = listingController.deleteListing(listingId, authentication);

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Listing tidak bisa dihapus saat auction masih aktif.", response.getBody());
    }

    @Test
    void deleteListing_shouldReturn403WhenAccessDenied() {
        doThrow(new AccessDeniedException("User tidak memiliki akses ke listing ini."))
                .when(listingService).deleteListing(listingId, sellerId, false);

        ResponseEntity<?> response = listingController.deleteListing(listingId, authentication);

        assertEquals(403, response.getStatusCode().value());
    }
}