package com.example.bidmart.wallet.strategy;

import com.example.bidmart.wallet.exception.InvalidRequestException;
import com.example.bidmart.wallet.model.PaymentMethod;
import com.example.bidmart.wallet.model.TransactionType;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

@Component
public class BankPaymentStrategy implements PaymentStrategy {

    private static final String REQUIRED_FIELD_BANK_NAME    = "bankName";
    private static final String REQUIRED_FIELD_ACCOUNT_NUM  = "accountNumber";

    @Override
    public boolean supports(PaymentMethod method) {
        return PaymentMethod.BANK == method;
    }

    @Override
    public void validateDetails(Map<String, String> details) {
        if (details == null
                || !details.containsKey(REQUIRED_FIELD_BANK_NAME)
                || !details.containsKey(REQUIRED_FIELD_ACCOUNT_NUM)) {
            throw new InvalidRequestException(
                    "Untuk metode BANK, 'bankName' dan 'accountNumber' wajib diisi.");
        }
    }

    @Override
    public String generateTransactionNote(TransactionType transactionType, BigDecimal amount, Map<String, String> details) {
        String bankName   = sanitize(details.get(REQUIRED_FIELD_BANK_NAME));
        String accountNum = sanitize(details.get(REQUIRED_FIELD_ACCOUNT_NUM));

        return transactionType == TransactionType.TOPUP
                ? "Top-Up via "  + bankName + " (Virtual Account: " + accountNum + ")"
                : "Withdraw to " + bankName + " (Rekening: "        + accountNum + ")";
    }

    /** Strips HTML-unsafe characters to prevent stored XSS in transaction notes. */
    private String sanitize(String input) {
        if (input == null) return "";
        return input.replaceAll("[<>\"'&]", "");
    }
}