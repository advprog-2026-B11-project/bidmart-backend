package com.example.bidmart.user.service;

import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.exceptions.CodeGenerationException;
import dev.samstevens.totp.time.SystemTimeProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MfaServiceImplTest {

    private MfaServiceImpl mfaService;

    @BeforeEach
    void setUp() {
        mfaService = new MfaServiceImpl();
    }

    @Test
    void generateMfaSecret_returnsNonEmptyString() {
        String secret = mfaService.generateMfaSecret();
        assertNotNull(secret);
        assertFalse(secret.trim().isEmpty());
        assertTrue(secret.length() >= 16); // TOTP secrets are typically >= 16 chars
    }

    @Test
    void getQrCodeImageUri_returnsValidDataUri() {
        String secret = mfaService.generateMfaSecret();
        String email = "test@example.com";

        String uri = mfaService.getQrCodeImageUri(secret, email);

        assertNotNull(uri);
        assertTrue(uri.startsWith("data:image/png;base64,"));
    }

    @Test
    void verifyCode_withValidCode_returnsTrue() throws CodeGenerationException {
        String secret = mfaService.generateMfaSecret();
        
        // Generate valid code for current time
        SystemTimeProvider timeProvider = new SystemTimeProvider();
        DefaultCodeGenerator generator = new DefaultCodeGenerator();
        long currentBucket = timeProvider.getTime() / 30; // 30 sec period
        String validCode = generator.generate(secret, currentBucket);

        boolean isValid = mfaService.verifyCode(secret, validCode);
        assertTrue(isValid);
    }

    @Test
    void verifyCode_withInvalidCode_returnsFalse() {
        String secret = mfaService.generateMfaSecret();
        String invalidCode = "123456"; // Unlikely to be valid

        boolean isValid = mfaService.verifyCode(secret, invalidCode);
        assertFalse(isValid);
    }
}
