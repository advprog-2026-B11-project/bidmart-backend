package com.example.bidmart.wallet.service;

import com.example.bidmart.wallet.model.Transaction;
import com.example.bidmart.wallet.model.Wallet;
import com.example.bidmart.wallet.repository.TransactionRepository;
import com.example.bidmart.wallet.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private WalletService walletService;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
    }

    @Test
    void createWallet_success() {
        when(walletRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(walletRepository.save(any(Wallet.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Wallet result = walletService.createWallet(userId);

        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        assertEquals(BigDecimal.ZERO, result.getBalanceAvailable());
        assertEquals(BigDecimal.ZERO, result.getBalanceLocked());
        verify(walletRepository).save(any(Wallet.class));
    }

    @Test
    void createWallet_alreadyExists_returnsNull() {
        when(walletRepository.findByUserId(userId))
                .thenReturn(Optional.of(new Wallet(userId)));

        Wallet result = walletService.createWallet(userId);

        assertNull(result);
        verify(walletRepository, never()).save(any(Wallet.class));
    }

    @Test
    void getWalletByUserId_found() {
        Wallet wallet = new Wallet(userId);
        when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));

        Wallet result = walletService.getWalletByUserId(userId);

        assertNotNull(result);
        assertEquals(userId, result.getUserId());
    }

    @Test
    void getWalletByUserId_notFound_returnsNull() {
        when(walletRepository.findByUserId(userId)).thenReturn(Optional.empty());

        Wallet result = walletService.getWalletByUserId(userId);

        assertNull(result);
    }

    @Test
    void topUp_success() {
        Wallet wallet = new Wallet(userId);
        BigDecimal amount = new BigDecimal("50000");

        when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Wallet result = walletService.topUp(userId, amount);

        assertNotNull(result);
        assertEquals(new BigDecimal("50000"), result.getBalanceAvailable());
        verify(walletRepository).save(any(Wallet.class));
    }

    @Test
    void topUp_zeroAmount_returnsNull() {
        Wallet result = walletService.topUp(userId, BigDecimal.ZERO);
        assertNull(result);
        verify(walletRepository, never()).save(any(Wallet.class));
    }

    @Test
    void topUp_negativeAmount_returnsNull() {
        Wallet result = walletService.topUp(userId, new BigDecimal("-100"));
        assertNull(result);
        verify(walletRepository, never()).save(any(Wallet.class));
    }

    @Test
    void topUp_nullAmount_returnsNull() {
        Wallet result = walletService.topUp(userId, null);
        assertNull(result);
    }

    @Test
    void topUp_walletNotFound_returnsNull() {
        when(walletRepository.findByUserId(userId)).thenReturn(Optional.empty());

        Wallet result = walletService.topUp(userId, new BigDecimal("10000"));

        assertNull(result);
        verify(walletRepository, never()).save(any(Wallet.class));
    }
}
