package com.example.bidmart.listing.dto;

import com.example.bidmart.common.validation.OnCreate;
import com.example.bidmart.listing.model.AuctionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record CreateListingRequest(
        @NotNull UUID categoryId,
        @NotBlank String title,
        @Pattern(regexp = ".*\\S.*") String description,
        @Pattern(regexp = ".*\\S.*") String imageUrl,
        @NotNull @DecimalMin("0.01") BigDecimal startingPrice,
        @DecimalMin("0.01") BigDecimal reservePrice,
        @NotNull @Future(groups = OnCreate.class) LocalDateTime endTime,
        AuctionType auctionType
) {
}