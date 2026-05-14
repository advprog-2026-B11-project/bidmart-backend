package com.example.bidmart.common.event;

import java.util.UUID;

public record AuctionExtendedEvent(
        UUID listingId,
        UUID sellerId
) {}
