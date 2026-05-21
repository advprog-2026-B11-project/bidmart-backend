package com.example.bidmart.order.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class CreateOrderRequest {
    @NotNull(message = "Listing ID tidak boleh kosong")
    private UUID listingId;

    @NotNull(message = "Buyer ID tidak boleh kosong")
    private UUID buyerId;

    @NotNull(message = "Seller ID tidak boleh kosong")
    private UUID sellerId;

    @NotNull(message = "Amount tidak boleh kosong")
    @Positive(message = "Amount harus lebih besar dari 0")
    private BigDecimal amount;
}