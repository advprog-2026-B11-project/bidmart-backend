package com.example.bidmart.listing.repository;

import com.example.bidmart.listing.model.AuctionStatus;
import com.example.bidmart.listing.model.Listing;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ListingRepository extends JpaRepository<Listing, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM Listing l WHERE l.id = :id")
    Optional<Listing> findByIdWithLock(@Param("id") UUID id);

    List<Listing> findByStatusInAndEndTimeBefore(List<AuctionStatus> statuses, LocalDateTime time);
    List<Listing> findByStatusIn(Collection<AuctionStatus> statuses);
    Page<Listing> findByStatusIn(Collection<AuctionStatus> statuses, Pageable pageable);

    default List<Listing> findActiveListings() {
        return findByStatusIn(Arrays.asList(AuctionStatus.ACTIVE, AuctionStatus.EXTENDED));
    }

    default Page<Listing> findActiveListings(Pageable pageable) {
        return findByStatusIn(Arrays.asList(AuctionStatus.ACTIVE, AuctionStatus.EXTENDED), pageable);
    }

    List<Listing> findBySellerIdAndStatusIn(UUID sellerId, List<AuctionStatus> statuses);

    @Query(value = "SELECT * FROM listings WHERE " +
            "(CAST(:keyword AS text) IS NULL OR LOWER(title) LIKE LOWER('%' || CAST(:keyword AS text) || '%') OR LOWER(description) LIKE LOWER('%' || CAST(:keyword AS text) || '%')) AND " +
            "(CAST(:category AS text) IS NULL OR category_id = CAST(:category AS uuid)) AND " +
            "(CAST(:minPrice AS numeric) IS NULL OR starting_price >= CAST(:minPrice AS numeric)) AND " +
            "(CAST(:maxPrice AS numeric) IS NULL OR starting_price <= CAST(:maxPrice AS numeric)) " +
            "ORDER BY created_at DESC",
            nativeQuery = true)
    List<Listing> findBySearchCriteria(@Param("keyword") String keyword, @Param("category") UUID category, @Param("minPrice") BigDecimal minPrice, @Param("maxPrice") BigDecimal maxPrice);

    @Query(value = "SELECT * FROM listings WHERE " +
            "(CAST(:keyword AS text) IS NULL OR LOWER(title) LIKE LOWER('%' || CAST(:keyword AS text) || '%') OR LOWER(description) LIKE LOWER('%' || CAST(:keyword AS text) || '%')) AND " +
            "(CAST(:category AS text) IS NULL OR category_id = CAST(:category AS uuid)) AND " +
            "(CAST(:minPrice AS numeric) IS NULL OR starting_price >= CAST(:minPrice AS numeric)) AND " +
            "(CAST(:maxPrice AS numeric) IS NULL OR starting_price <= CAST(:maxPrice AS numeric)) " +
            "ORDER BY created_at DESC",
            countQuery = "SELECT count(*) FROM listings WHERE " +
            "(CAST(:keyword AS text) IS NULL OR LOWER(title) LIKE LOWER('%' || CAST(:keyword AS text) || '%') OR LOWER(description) LIKE LOWER('%' || CAST(:keyword AS text) || '%')) AND " +
            "(CAST(:category AS text) IS NULL OR category_id = CAST(:category AS uuid)) AND " +
            "(CAST(:minPrice AS numeric) IS NULL OR starting_price >= CAST(:minPrice AS numeric)) AND " +
            "(CAST(:maxPrice AS numeric) IS NULL OR starting_price <= CAST(:maxPrice AS numeric))",
            nativeQuery = true)
    Page<Listing> findBySearchCriteria(@Param("keyword") String keyword, @Param("category") UUID category, @Param("minPrice") BigDecimal minPrice, @Param("maxPrice") BigDecimal maxPrice, Pageable pageable);
}
