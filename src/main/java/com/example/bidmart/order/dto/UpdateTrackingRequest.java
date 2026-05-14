package com.example.bidmart.order.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateTrackingRequest {
    @NotBlank(message = "Nomor resi tidak boleh kosong")
    private String trackingNumber;
}