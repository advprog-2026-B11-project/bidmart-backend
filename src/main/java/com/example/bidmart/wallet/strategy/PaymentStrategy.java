package com.example.bidmart.wallet.strategy;

import com.example.bidmart.wallet.model.PaymentMethod;
import com.example.bidmart.wallet.model.TransactionType;
import java.math.BigDecimal;
import java.util.Map;

public interface PaymentStrategy {
    boolean supports(PaymentMethod method);

    void validateDetails(Map<String, String> details);

    String generateTransactionNote(TransactionType transactionType, BigDecimal amount, Map<String, String> details);
}
