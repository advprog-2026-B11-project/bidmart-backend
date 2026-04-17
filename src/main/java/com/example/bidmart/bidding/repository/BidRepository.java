package com.example.bidmart.bidding.repository;

import com.example.bidmart.bidding.model.Bid;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BidRepository extends JpaRepository<Bid, UUID> {

    List<Bid> findByListingIdOrderByCreatedAtDesc(UUID listingId);

    List<Bid> findByBuyerIdOrderByCreatedAtDesc(UUID buyerId);

    Optional<Bid> findTopByListingIdOrderByAmountDescCreatedAtAsc(UUID listingId);

    Optional<Bid> findTopByListingIdAndBuyerIdOrderByCreatedAtDesc(UUID listingId, UUID buyerId);
}
