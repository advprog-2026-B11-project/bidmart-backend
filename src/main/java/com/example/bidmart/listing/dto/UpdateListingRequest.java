package com.example.bidmart.listing.dto;

import com.example.bidmart.listing.model.AuctionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record UpdateListingRequest(
        @NotNull UUID categoryId,
        @NotBlank String title,
        String description,
        String imageUrl,
        @NotNull @DecimalMin("0.01") BigDecimal startingPrice,
        @DecimalMin("0.01") BigDecimal reservePrice,
        @NotNull @Future LocalDateTime endTime,
        AuctionType auctionType
) {
}