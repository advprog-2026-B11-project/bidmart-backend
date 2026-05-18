package com.example.bidmart.listing.listener;

import com.example.bidmart.common.event.UserDeactivatedEvent;
import com.example.bidmart.listing.model.AuctionStatus;
import com.example.bidmart.listing.model.Listing;
import com.example.bidmart.listing.repository.ListingRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

@Slf4j
@Component
public class ListingEventListener {

    private final ListingRepository listingRepository;

    public ListingEventListener(ListingRepository listingRepository) {
        this.listingRepository = listingRepository;
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserDeactivated(UserDeactivatedEvent event) {
        log.info("Event Received [UserDeactivated]: Cancelling active listings for user [{}]", event.userId());

        List<Listing> activeListings = listingRepository.findBySellerIdAndStatusIn(
                event.userId(),
                List.of(AuctionStatus.ACTIVE, AuctionStatus.EXTENDED)
        );

        for (Listing listing : activeListings) {
            listing.setStatus(AuctionStatus.UNSOLD);
            listingRepository.save(listing);
        }

        log.info("Cancelled [{}] active listings for user [{}]", activeListings.size(), event.userId());
    }
}