package com.example.bidmart.listing.repository;

import com.example.bidmart.listing.model.AuctionStatus;
import com.example.bidmart.listing.model.Listing;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ListingRepository extends JpaRepository<Listing, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM Listing l WHERE l.id = :id")
    Optional<Listing> findByIdWithLock(@Param("id") UUID id);

    List<Listing> findByStatusInAndEndTimeBefore(List<AuctionStatus> statuses, LocalDateTime time);
    List<Listing> findByStatusIn(Collection<AuctionStatus> statuses);

    default List<Listing> findActiveListings() {
        return findByStatusIn(Arrays.asList(AuctionStatus.ACTIVE, AuctionStatus.EXTENDED));
    }

    List<Listing> findBySellerIdAndStatusIn(UUID sellerId, List<AuctionStatus> statuses);

    @Query("""
            SELECT l FROM Listing l
            WHERE (:keyword IS NULL
                OR LOWER(l.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(l.description) LIKE LOWER(CONCAT('%', :keyword, '%')))
            AND (:category IS NULL OR l.categoryId = :category)
            AND (:minPrice IS NULL OR l.startingPrice >= :minPrice)
            AND (:maxPrice IS NULL OR l.startingPrice <= :maxPrice)
            """)
    List<Listing> findBySearchCriteria(
            @Param("keyword") String keyword,
            @Param("category") UUID category,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice
    );
}
