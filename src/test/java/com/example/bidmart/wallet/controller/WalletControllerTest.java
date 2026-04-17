package com.example.bidmart.wallet.controller;

import com.example.bidmart.wallet.dto.*;
import com.example.bidmart.wallet.model.Transaction;
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
import java.util.List;
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
    private UUID listingId;
    private Wallet wallet;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        listingId = UUID.randomUUID();
        wallet = new Wallet(userId);
        wallet.setBalanceAvailable(new BigDecimal("100000"));
    }

    @Test
    void createWallet_success() {
        when(walletService.createWallet(userId)).thenReturn(wallet);

        ResponseEntity<Wallet> response = walletController.createWallet(new CreateWalletRequest(userId));

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void createWallet_conflict() {
        when(walletService.createWallet(userId)).thenReturn(null);

        ResponseEntity<Wallet> response = walletController.createWallet(new CreateWalletRequest(userId));

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }

    @Test
    void getBalance_success() {
        when(walletService.getWalletByUserId(userId)).thenReturn(wallet);

        ResponseEntity<Wallet> response = walletController.getBalance(userId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(new BigDecimal("100000"), response.getBody().getBalanceAvailable());
    }

    @Test
    void getBalance_notFound() {
        when(walletService.getWalletByUserId(userId)).thenReturn(null);

        assertEquals(HttpStatus.NOT_FOUND, walletController.getBalance(userId).getStatusCode());
    }

    @Test
    void topUp_success() {
        wallet.setBalanceAvailable(new BigDecimal("150000"));
        when(walletService.topUp(eq(userId), any())).thenReturn(wallet);

        ResponseEntity<Wallet> response = walletController.topUp(userId, new TopUpRequest(new BigDecimal("50000")));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(new BigDecimal("150000"), response.getBody().getBalanceAvailable());
    }

    @Test
    void topUp_badRequest() {
        when(walletService.topUp(eq(userId), any())).thenReturn(null);

        assertEquals(HttpStatus.BAD_REQUEST,
                walletController.topUp(userId, new TopUpRequest(new BigDecimal("-1"))).getStatusCode());
    }

    @Test
    void holdBalance_success() {
        wallet.setBalanceAvailable(new BigDecimal("70000"));
        wallet.setBalanceLocked(new BigDecimal("30000"));
        when(walletService.reserveBidFunds(eq(userId), eq(listingId), any())).thenReturn(wallet);

        HoldBalanceRequest request = new HoldBalanceRequest(new BigDecimal("30000"), listingId);
        ResponseEntity<Wallet> response = walletController.holdBalance(userId, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(new BigDecimal("30000"), response.getBody().getBalanceLocked());
    }

    @Test
    void holdBalance_badRequest() {
        when(walletService.reserveBidFunds(eq(userId), eq(listingId), any())).thenReturn(null);

        HoldBalanceRequest request = new HoldBalanceRequest(new BigDecimal("999999"), listingId);

        assertEquals(HttpStatus.BAD_REQUEST,
                walletController.holdBalance(userId, request).getStatusCode());
    }

    @Test
    void releaseHold_success() {
        wallet.setBalanceLocked(BigDecimal.ZERO);
        when(walletService.releaseBidFunds(eq(userId), eq(listingId), any())).thenReturn(wallet);

        HoldBalanceRequest request = new HoldBalanceRequest(new BigDecimal("30000"), listingId);
        ResponseEntity<Wallet> response = walletController.releaseHold(userId, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void releaseHold_badRequest() {
        when(walletService.releaseBidFunds(eq(userId), eq(listingId), any())).thenReturn(null);

        assertEquals(HttpStatus.BAD_REQUEST,
                walletController.releaseHold(userId,
                        new HoldBalanceRequest(new BigDecimal("30000"), listingId)).getStatusCode());
    }

    @Test
    void settlePayment_success() {
        when(walletService.settlePayment(eq(userId), any(), any())).thenReturn(wallet);

        HoldBalanceRequest request = new HoldBalanceRequest(new BigDecimal("30000"), listingId);
        ResponseEntity<Wallet> response = walletController.settlePayment(userId, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void settlePayment_badRequest() {
        when(walletService.settlePayment(eq(userId), any(), any())).thenReturn(null);

        assertEquals(HttpStatus.BAD_REQUEST,
                walletController.settlePayment(userId,
                        new HoldBalanceRequest(new BigDecimal("30000"), listingId)).getStatusCode());
    }

    @Test
    void withdraw_success() {
        wallet.setBalanceAvailable(new BigDecimal("60000"));
        when(walletService.withdraw(eq(userId), any())).thenReturn(wallet);

        ResponseEntity<Wallet> response = walletController.withdraw(userId, new WithdrawRequest(new BigDecimal("40000")));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(new BigDecimal("60000"), response.getBody().getBalanceAvailable());
    }

    @Test
    void withdraw_badRequest() {
        when(walletService.withdraw(eq(userId), any())).thenReturn(null);

        assertEquals(HttpStatus.BAD_REQUEST,
                walletController.withdraw(userId, new WithdrawRequest(new BigDecimal("999999"))).getStatusCode());
    }

    @Test
    void getTransactionHistory_success() {
        Transaction tx = new Transaction(UUID.randomUUID(), "TOPUP", new BigDecimal("50000"), null);
        when(walletService.getTransactionHistory(userId)).thenReturn(List.of(tx));

        ResponseEntity<List<Transaction>> response = walletController.getTransactionHistory(userId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void getTransactionHistory_notFound() {
        when(walletService.getTransactionHistory(userId)).thenReturn(null);

        assertEquals(HttpStatus.NOT_FOUND,
                walletController.getTransactionHistory(userId).getStatusCode());
    }
}
