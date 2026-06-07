package com.example.bidmart.common.util;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Generates deterministic idempotency keys for financial operations.
 * Same input parameters always produce the same key, preventing
 * duplicate processing of identical operations.
 */
public final class IdempotencyKeyGenerator {

    private IdempotencyKeyGenerator() {
        // Utility class — prevent instantiation
    }

    /**
     * Generates a deterministic UUID-based idempotency key.
     *
     * @param operation the operation type (e.g., "BID_HOLD", "AUCTION_SETTLE")
     * @param parts     context-specific parameters (userId, listingId, amount, etc.)
     * @return a deterministic UUID string
     */
    public static String generate(String operation, Object... parts) {
        StringBuilder sb = new StringBuilder(operation);
        for (Object part : parts) {
            sb.append(":");
            if (part instanceof BigDecimal) {
                sb.append(((BigDecimal) part).toPlainString());
            } else {
                sb.append(part);
            }
        }
        return UUID.nameUUIDFromBytes(
                sb.toString().getBytes(StandardCharsets.UTF_8)
        ).toString();
    }
}
