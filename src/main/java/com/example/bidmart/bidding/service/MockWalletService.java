package com.example.bidmart.bidding.service;

import com.example.bidmart.bidding.dto.MockWalletStateResponse;
import com.example.bidmart.bidding.exception.BidValidationException;
import com.example.bidmart.bidding.exception.InsufficientBalanceException;
import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class MockWalletService {

    public static final UUID DEFAULT_BUYER_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    private static final String KEY_SEPARATOR = "::";

    private final Map<UUID, BigDecimal> availableBalances = new ConcurrentHashMap<>();
    private final Map<String, BigDecimal> lockedBalances = new ConcurrentHashMap<>();

    @PostConstruct
    public void initializeDefaults() {
        if (!availableBalances.isEmpty()) {
            return;
        }

        availableBalances.put(DEFAULT_BUYER_ID, new BigDecimal("1000000.00"));
    }

    public synchronized void setAvailableBalance(UUID buyerId, BigDecimal availableBalance) {
        validateBuyerId(buyerId);

        if (availableBalance == null || availableBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new BidValidationException("Saldo mock wallet tidak valid.");
        }

        availableBalances.put(buyerId, availableBalance);
    }

    public synchronized void reserveBidFunds(UUID buyerId, UUID listingId, BigDecimal reserveTarget) {
        validateBuyerId(buyerId);
        validateListingId(listingId);

        if (reserveTarget == null || reserveTarget.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BidValidationException("Nominal dana yang dikunci harus lebih dari 0.");
        }

        String key = buildKey(buyerId, listingId);
        BigDecimal currentlyLocked = lockedBalances.getOrDefault(key, BigDecimal.ZERO);

        if (reserveTarget.compareTo(currentlyLocked) <= 0) {
            return;
        }

        BigDecimal additionalLock = reserveTarget.subtract(currentlyLocked);
        BigDecimal available = availableBalances.getOrDefault(buyerId, BigDecimal.ZERO);

        if (available.compareTo(additionalLock) < 0) {
            throw new InsufficientBalanceException(
                    "Saldo tidak mencukupi. Dibutuhkan tambahan " + additionalLock + ", tersedia " + available + "."
            );
        }

        availableBalances.put(buyerId, available.subtract(additionalLock));
        lockedBalances.put(key, currentlyLocked.add(additionalLock));
    }

    public synchronized void releaseBidFunds(UUID buyerId, UUID listingId, BigDecimal releaseAmount) {
        validateBuyerId(buyerId);
        validateListingId(listingId);

        if (releaseAmount == null || releaseAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        String key = buildKey(buyerId, listingId);
        BigDecimal currentlyLocked = lockedBalances.getOrDefault(key, BigDecimal.ZERO);

        if (currentlyLocked.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        BigDecimal actualRelease = currentlyLocked.min(releaseAmount);
        BigDecimal remainingLocked = currentlyLocked.subtract(actualRelease);

        BigDecimal available = availableBalances.getOrDefault(buyerId, BigDecimal.ZERO);
        availableBalances.put(buyerId, available.add(actualRelease));

        if (remainingLocked.compareTo(BigDecimal.ZERO) == 0) {
            lockedBalances.remove(key);
            return;
        }

        lockedBalances.put(key, remainingLocked);
    }

    public synchronized MockWalletStateResponse getWalletState(UUID buyerId) {
        validateBuyerId(buyerId);

        Map<UUID, BigDecimal> lockedByListing = lockedBalances.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(buyerId + KEY_SEPARATOR))
                .collect(Collectors.toMap(
                        entry -> UUID.fromString(entry.getKey().split(KEY_SEPARATOR, 2)[1]),
                        Map.Entry::getValue,
                        (existing, replacement) -> replacement,
                        LinkedHashMap::new
                ));

        BigDecimal available = availableBalances.getOrDefault(buyerId, BigDecimal.ZERO);
        return new MockWalletStateResponse(buyerId, available, lockedByListing);
    }

    private String buildKey(UUID buyerId, UUID listingId) {
        return buyerId + KEY_SEPARATOR + listingId;
    }

    private void validateBuyerId(UUID buyerId) {
        if (buyerId == null) {
            throw new BidValidationException("buyerId wajib diisi.");
        }
    }

    private void validateListingId(UUID listingId) {
        if (listingId == null) {
            throw new BidValidationException("listingId wajib diisi.");
        }
    }
}
