package com.example.bidmart.bidding.service;

import com.example.bidmart.bidding.dto.MockListingUpsertRequest;
import com.example.bidmart.bidding.exception.BidValidationException;
import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class MockListingService {

    public static final UUID DEFAULT_LISTING_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    public static final UUID DEFAULT_SELLER_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    private final Map<UUID, ListingSnapshot> listings = new ConcurrentHashMap<>();

    @PostConstruct
    public void initializeDefaults() {
        if (!listings.isEmpty()) {
            return;
        }

        listings.put(
                DEFAULT_LISTING_ID,
                new ListingSnapshot(
                        DEFAULT_LISTING_ID,
                        DEFAULT_SELLER_ID,
                        new BigDecimal("10000.00"),
                        LocalDateTime.now().plusDays(2),
                        "ACTIVE"
                )
        );
    }

    public Optional<ListingSnapshot> findById(UUID listingId) {
        return Optional.ofNullable(listings.get(listingId));
    }

    public List<ListingSnapshot> findAll() {
        return listings.values().stream()
                .sorted(Comparator.comparing(ListingSnapshot::id))
                .toList();
    }

    public ListingSnapshot upsert(MockListingUpsertRequest request, UUID authenticatedSellerId) {
        if (request == null) {
            throw new BidValidationException("Request mock listing tidak boleh null.");
        }

        if (authenticatedSellerId == null) {
            throw new BidValidationException("sellerId wajib diisi.");
        }

        BigDecimal startingPrice = request.startingPrice() == null ? BigDecimal.ZERO : request.startingPrice();
        if (startingPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new BidValidationException("Starting price mock listing tidak boleh negatif.");
        }

        UUID requestedSellerId = request.sellerId();
        if (requestedSellerId != null && !requestedSellerId.equals(authenticatedSellerId)) {
            throw new BidValidationException("sellerId pada payload harus sama dengan user yang sedang login.");
        }

        UUID listingId = request.id() == null ? UUID.randomUUID() : request.id();
        UUID sellerId = authenticatedSellerId;
        LocalDateTime endTime = request.endTime() == null ? LocalDateTime.now().plusDays(1) : request.endTime();
        String status = request.status() == null || request.status().isBlank() ? "ACTIVE" : request.status();

        ListingSnapshot snapshot = new ListingSnapshot(listingId, sellerId, startingPrice, endTime, status);
        listings.put(listingId, snapshot);
        return snapshot;
    }
}
