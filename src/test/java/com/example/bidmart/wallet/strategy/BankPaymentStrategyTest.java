package com.example.bidmart.wallet.strategy;

import com.example.bidmart.wallet.exception.InvalidRequestException;
import com.example.bidmart.wallet.model.PaymentMethod;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BankPaymentStrategyTest {

    private final BankPaymentStrategy strategy = new BankPaymentStrategy();

    @Test void supportsBank() { assertTrue(strategy.supports(PaymentMethod.BANK)); }
    @Test void doesNotSupportGopay() { assertFalse(strategy.supports(PaymentMethod.GOPAY)); }

    @Test void validateDetails_success() {
        assertDoesNotThrow(() -> strategy.validateDetails(Map.of("bankName", "BCA", "accountNumber", "123")));
    }
    @Test void validateDetails_nullDetails_throws() {
        assertThrows(InvalidRequestException.class, () -> strategy.validateDetails(null));
    }
    @Test void validateDetails_missingBankName_throws() {
        assertThrows(InvalidRequestException.class, () -> strategy.validateDetails(Map.of("accountNumber", "123")));
    }
    @Test void validateDetails_missingAccountNumber_throws() {
        assertThrows(InvalidRequestException.class, () -> strategy.validateDetails(Map.of("bankName", "BCA")));
    }
    @Test void validateDetails_emptyMap_throws() {
        assertThrows(InvalidRequestException.class, () -> strategy.validateDetails(new HashMap<>()));
    }

    @Test void generateNote_topUp() {
        String note = strategy.generateTransactionNote("TOPUP", new BigDecimal("50000"),
                Map.of("bankName", "BCA", "accountNumber", "123"));
        assertTrue(note.contains("Top-Up"));
        assertTrue(note.contains("BCA"));
    }
    @Test void generateNote_withdraw() {
        String note = strategy.generateTransactionNote("WITHDRAW", new BigDecimal("50000"),
                Map.of("bankName", "Mandiri", "accountNumber", "456"));
        assertTrue(note.contains("Withdraw"));
        assertTrue(note.contains("Mandiri"));
    }
}
