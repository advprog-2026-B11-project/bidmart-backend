package com.example.bidmart.order.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ResolveDisputeRequest {
    @NotNull(message = "Pilihan refund tidak boleh kosong")
    private Boolean refundBuyer;
}