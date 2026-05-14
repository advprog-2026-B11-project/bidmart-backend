package com.example.bidmart.order.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateOrderStatusRequest {
    @NotBlank(message = "Status pesanan tidak boleh kosong")
    private String status;
}