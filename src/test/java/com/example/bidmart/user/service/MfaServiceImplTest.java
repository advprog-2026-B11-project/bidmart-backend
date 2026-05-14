package com.example.bidmart.user.service;

import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MfaServiceImplTest {

    @Test
    void generateSecretAndVerifyCode_shouldSucceed() throws Exception {
        MfaServiceImpl mfaService = new MfaServiceImpl();
        String secret = mfaService.generateMfaSecret();

        assertNotNull(secret);
        assertFalse(secret.isBlank());

        TimeProvider timeProvider = new SystemTimeProvider();
        DefaultCodeGenerator codeGenerator = new DefaultCodeGenerator();
        long currentBucket = Math.floorDiv(timeProvider.getTime(), 30L);

        String code = codeGenerator.generate(secret, currentBucket);

        assertTrue(mfaService.verifyCode(secret, code));
    }

    @Test
    void getQrCodeImageUri_shouldReturnDataUri() {
        MfaServiceImpl mfaService = new MfaServiceImpl();
        String secret = mfaService.generateMfaSecret();

        String dataUri = mfaService.getQrCodeImageUri(secret, "alice@mail.com");

        assertNotNull(dataUri);
        assertTrue(dataUri.startsWith("data:image/png;base64,"));
    }
}
