package com.example.bidmart.bidding.dto;

import java.math.BigDecimal;

public record MockWalletBalanceRequest(
        BigDecimal availableBalance
) {
}
