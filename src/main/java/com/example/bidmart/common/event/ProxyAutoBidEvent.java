package com.example.bidmart.common.event;

import java.math.BigDecimal;
import java.util.UUID;

/** Published when a proxy bid automatically counters a challenger or wins a proxy-vs-proxy resolution. */
public record ProxyAutoBidEvent(
        UUID listingId,
        UUID proxyOwnerId,
        BigDecimal newVisiblePrice
) {}
