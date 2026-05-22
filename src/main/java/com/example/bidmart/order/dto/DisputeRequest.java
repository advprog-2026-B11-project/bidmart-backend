package com.example.bidmart.order.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DisputeRequest {
    @NotBlank(message = "Alasan sengketa tidak boleh kosong")
    private String reason;
}