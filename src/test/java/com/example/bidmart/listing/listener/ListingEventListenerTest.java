package com.example.bidmart.listing.listener;

import com.example.bidmart.common.event.UserDeactivatedEvent;
import com.example.bidmart.listing.model.AuctionStatus;
import com.example.bidmart.listing.model.Listing;
import com.example.bidmart.listing.repository.ListingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListingEventListenerTest {

    @Mock
    private ListingRepository listingRepository;

    @InjectMocks
    private ListingEventListener listener;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
    }

    @Test
    void onUserDeactivated_updatesListingsToUnsold() {
        Listing listing1 = new Listing();
        listing1.setId(UUID.randomUUID());
        listing1.setStatus(AuctionStatus.ACTIVE);

        Listing listing2 = new Listing();
        listing2.setId(UUID.randomUUID());
        listing2.setStatus(AuctionStatus.EXTENDED);

        List<Listing> activeListings = List.of(listing1, listing2);

        when(listingRepository.findBySellerIdAndStatusIn(eq(userId), anyList()))
                .thenReturn(activeListings);

        UserDeactivatedEvent event = new UserDeactivatedEvent(userId, "testuser", Instant.now());

        listener.onUserDeactivated(event);

        verify(listingRepository).save(listing1);
        verify(listingRepository).save(listing2);
        assert listing1.getStatus() == AuctionStatus.UNSOLD;
        assert listing2.getStatus() == AuctionStatus.UNSOLD;
    }
}
