package com.example.bidmart.wallet.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.bidmart.user.service.UserService;
import com.example.bidmart.wallet.dto.TopUpRequest;
import com.example.bidmart.wallet.model.Wallet;
import com.example.bidmart.wallet.service.WalletService;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
class WalletControllerTest {

    @Mock
    private WalletService walletService;

    @Mock
    private UserService userService;

    @InjectMocks
    private WalletController walletController;

    private UUID userId;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("walletUser");
        when(userService.getUserIdByUsername("walletUser")).thenReturn(userId);
    }

    @Test
    void createWallet_success() {
        Wallet wallet = new Wallet(userId);
        when(walletService.createWallet(userId)).thenReturn(wallet);

        ResponseEntity<Wallet> response = walletController.createWallet(authentication);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(userId, response.getBody().getUserId());
    }

    @Test
    void createWallet_alreadyExists() {
        when(walletService.createWallet(userId)).thenReturn(null);

        ResponseEntity<Wallet> response = walletController.createWallet(authentication);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }

    @Test
    void getMyBalance_success() {
        Wallet wallet = new Wallet(userId);
        when(walletService.getWalletByUserId(userId)).thenReturn(wallet);

        ResponseEntity<Wallet> response = walletController.getMyBalance(authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(userId, response.getBody().getUserId());
        assertEquals(BigDecimal.ZERO, response.getBody().getBalanceAvailable());
    }

    @Test
    void getMyBalance_notFound() {
        when(walletService.getWalletByUserId(userId)).thenReturn(null);

        ResponseEntity<Wallet> response = walletController.getMyBalance(authentication);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void topUpMyWallet_success() {
        Wallet wallet = new Wallet(userId);
        wallet.setBalanceAvailable(new BigDecimal("50000"));
        when(walletService.topUp(eq(userId), any(BigDecimal.class))).thenReturn(wallet);

        TopUpRequest request = new TopUpRequest(new BigDecimal("50000"));
        ResponseEntity<Wallet> response = walletController.topUpMyWallet(authentication, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(new BigDecimal("50000"), response.getBody().getBalanceAvailable());
    }

    @Test
    void topUpMyWallet_failed() {
        when(walletService.topUp(eq(userId), any(BigDecimal.class))).thenReturn(null);

        TopUpRequest request = new TopUpRequest(new BigDecimal("-100"));
        ResponseEntity<Wallet> response = walletController.topUpMyWallet(authentication, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }
}
