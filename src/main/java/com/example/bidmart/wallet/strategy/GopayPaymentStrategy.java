package com.example.bidmart.wallet.strategy;

import com.example.bidmart.wallet.exception.InvalidRequestException;
import com.example.bidmart.wallet.model.PaymentMethod;
import com.example.bidmart.wallet.model.TransactionType;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.regex.Pattern;

@Component
public class GopayPaymentStrategy implements PaymentStrategy {

    private static final String REQUIRED_FIELD_PHONE = "phoneNumber";
    private static final Pattern INDONESIAN_PHONE_PATTERN =
            Pattern.compile("^(\\+62|62|08)\\d{8,13}$");

    @Override
    public boolean supports(PaymentMethod method) {
        return PaymentMethod.GOPAY == method;
    }

    @Override
    public void validateDetails(Map<String, String> details) {
        requirePhoneNumber(details);
        validatePhoneFormat(details.get(REQUIRED_FIELD_PHONE));
    }

    @Override
    public String generateTransactionNote(TransactionType transactionType, BigDecimal amount, Map<String, String> details) {
        String maskedPhone = maskPhoneNumber(details.get(REQUIRED_FIELD_PHONE));

        return transactionType == TransactionType.TOPUP
                ? "Top-Up via GoPay ("  + maskedPhone + ")"
                : "Withdraw to GoPay (" + maskedPhone + ")";
    }

    private void requirePhoneNumber(Map<String, String> details) {
        if (details == null || !details.containsKey(REQUIRED_FIELD_PHONE)) {
            throw new InvalidRequestException("Untuk metode GOPAY, 'phoneNumber' wajib diisi.");
        }
    }

    private void validatePhoneFormat(String rawPhone) {
        // Strip whitespace and hyphens before validation
        String normalised = rawPhone.replaceAll("[\\s\\-]", "");
        if (!INDONESIAN_PHONE_PATTERN.matcher(normalised).matches()) {
            throw new InvalidRequestException(
                    "Nomor telepon tidak valid. Gunakan format 08xx, +62xx, atau 62xx (10-15 digit).");
        }
    }

    /**
     * Masks the middle digits of a phone number for privacy in transaction notes.
     * Example: "081234567890" → "0812****7890"
     * Falls back to "****" if the number is too short to safely mask.
     */
    private String maskPhoneNumber(String phone) {
        if (phone == null || phone.length() <= 8) return "****";
        return phone.substring(0, 4) + "****" + phone.substring(phone.length() - 4);
    }
}
