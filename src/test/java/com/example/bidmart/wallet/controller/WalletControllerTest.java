package com.example.bidmart.wallet.controller;

import com.example.bidmart.wallet.dto.CreateWalletRequest;
import com.example.bidmart.wallet.dto.TopUpRequest;
import com.example.bidmart.wallet.model.Wallet;
import com.example.bidmart.wallet.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WalletControllerTest {

    @Mock
    private WalletService walletService;

    @InjectMocks
    private WalletController walletController;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
    }

    @Test
    void createWallet_success() {
        Wallet wallet = new Wallet(userId);
        when(walletService.createWallet(userId)).thenReturn(wallet);

        CreateWalletRequest request = new CreateWalletRequest(userId);
        ResponseEntity<Wallet> response = walletController.createWallet(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(userId, response.getBody().getUserId());
    }

    @Test
    void createWallet_alreadyExists() {
        when(walletService.createWallet(userId)).thenReturn(null);

        CreateWalletRequest request = new CreateWalletRequest(userId);
        ResponseEntity<Wallet> response = walletController.createWallet(request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }

    @Test
    void getBalance_success() {
        Wallet wallet = new Wallet(userId);
        when(walletService.getWalletByUserId(userId)).thenReturn(wallet);

        ResponseEntity<Wallet> response = walletController.getBalance(userId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(userId, response.getBody().getUserId());
        assertEquals(BigDecimal.ZERO, response.getBody().getBalanceAvailable());
    }

    @Test
    void getBalance_notFound() {
        when(walletService.getWalletByUserId(userId)).thenReturn(null);

        ResponseEntity<Wallet> response = walletController.getBalance(userId);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void topUp_success() {
        Wallet wallet = new Wallet(userId);
        wallet.setBalanceAvailable(new BigDecimal("50000"));
        when(walletService.topUp(eq(userId), any(BigDecimal.class))).thenReturn(wallet);

        TopUpRequest request = new TopUpRequest(new BigDecimal("50000"));
        ResponseEntity<Wallet> response = walletController.topUp(userId, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(new BigDecimal("50000"), response.getBody().getBalanceAvailable());
    }

    @Test
    void topUp_failed() {
        when(walletService.topUp(eq(userId), any(BigDecimal.class))).thenReturn(null);

        TopUpRequest request = new TopUpRequest(new BigDecimal("-100"));
        ResponseEntity<Wallet> response = walletController.topUp(userId, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }
}
