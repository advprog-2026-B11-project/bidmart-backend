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
import java.util.List;
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
    private Wallet wallet;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        wallet = new Wallet(userId);
        wallet.setBalanceAvailable(new BigDecimal("100000"));
        wallet.setBalanceLocked(BigDecimal.ZERO);
    }

    // === createWallet ===

    @Test
    void createWallet_success() {
        when(walletRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(walletRepository.save(any(Wallet.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Wallet result = walletService.createWallet(userId);

        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        verify(walletRepository).save(any(Wallet.class));
    }

    @Test
    void createWallet_alreadyExists_returnsNull() {
        when(walletRepository.findByUserId(userId))
                .thenReturn(Optional.of(wallet));

        assertNull(walletService.createWallet(userId));
        verify(walletRepository, never()).save(any());
    }

    // === getWalletByUserId ===

    @Test
    void getWalletByUserId_found() {
        when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));

        Wallet result = walletService.getWalletByUserId(userId);

        assertNotNull(result);
        assertEquals(userId, result.getUserId());
    }

    @Test
    void getWalletByUserId_notFound() {
        when(walletRepository.findByUserId(userId)).thenReturn(Optional.empty());
        assertNull(walletService.getWalletByUserId(userId));
    }

    // === topUp ===

    @Test
    void topUp_success_recordsTransaction() {
        when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Wallet result = walletService.topUp(userId, new BigDecimal("50000"));

        assertNotNull(result);
        assertEquals(new BigDecimal("150000"), result.getBalanceAvailable());
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void topUp_zeroAmount_returnsNull() {
        assertNull(walletService.topUp(userId, BigDecimal.ZERO));
        verify(walletRepository, never()).save(any());
    }

    @Test
    void topUp_negativeAmount_returnsNull() {
        assertNull(walletService.topUp(userId, new BigDecimal("-100")));
    }

    @Test
    void topUp_nullAmount_returnsNull() {
        assertNull(walletService.topUp(userId, null));
    }

    @Test
    void topUp_walletNotFound_returnsNull() {
        when(walletRepository.findByUserId(userId)).thenReturn(Optional.empty());
        assertNull(walletService.topUp(userId, new BigDecimal("10000")));
    }

    // === reserveBidFunds ===

    @Test
    void reserveBidFunds_freshHold_success() {
        UUID listingId = UUID.randomUUID();
        when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));
        when(transactionRepository.findByWalletIdAndReferenceId(any(), any())).thenReturn(List.of());

        Wallet result = walletService.reserveBidFunds(userId, listingId, new BigDecimal("30000"));

        assertNotNull(result);
        assertEquals(new BigDecimal("70000"), result.getBalanceAvailable());
        assertEquals(new BigDecimal("30000"), result.getBalanceLocked());
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void reserveBidFunds_incrementalHold_onlyLocksAdditional() {
        UUID listingId = UUID.randomUUID();
        wallet.setBalanceAvailable(new BigDecimal("70000"));
        wallet.setBalanceLocked(new BigDecimal("30000"));

        Transaction prevHold = new Transaction(wallet.getId(), "HOLD", new BigDecimal("30000"), listingId.toString());
        when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));
        when(transactionRepository.findByWalletIdAndReferenceId(any(), any())).thenReturn(List.of(prevHold));

        Wallet result = walletService.reserveBidFunds(userId, listingId, new BigDecimal("50000"));

        assertNotNull(result);
        assertEquals(new BigDecimal("50000"), result.getBalanceAvailable());
        assertEquals(new BigDecimal("50000"), result.getBalanceLocked());
    }

    @Test
    void reserveBidFunds_targetAlreadyMet_noAdditionalLock() {
        UUID listingId = UUID.randomUUID();

        Transaction prevHold = new Transaction(wallet.getId(), "HOLD", new BigDecimal("50000"), listingId.toString());
        when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));
        when(transactionRepository.findByWalletIdAndReferenceId(any(), any())).thenReturn(List.of(prevHold));

        Wallet result = walletService.reserveBidFunds(userId, listingId, new BigDecimal("30000"));

        assertNotNull(result);
        verify(walletRepository, never()).save(any());
    }

    @Test
    void reserveBidFunds_insufficientBalance_returnsNull() {
        UUID listingId = UUID.randomUUID();
        when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));
        when(transactionRepository.findByWalletIdAndReferenceId(any(), any())).thenReturn(List.of());

        assertNull(walletService.reserveBidFunds(userId, listingId, new BigDecimal("200000")));
        verify(walletRepository, never()).save(any());
    }

    @Test
    void reserveBidFunds_walletNotFound_returnsNull() {
        when(walletRepository.findByUserId(userId)).thenReturn(Optional.empty());
        assertNull(walletService.reserveBidFunds(userId, UUID.randomUUID(), new BigDecimal("10000")));
    }

    @Test
    void reserveBidFunds_invalidAmount_returnsNull() {
        assertNull(walletService.reserveBidFunds(userId, UUID.randomUUID(), BigDecimal.ZERO));
        assertNull(walletService.reserveBidFunds(userId, UUID.randomUUID(), null));
    }

    // === releaseBidFunds ===

    @Test
    void releaseBidFunds_success_releasesHeldAmount() {
        UUID listingId = UUID.randomUUID();
        wallet.setBalanceAvailable(new BigDecimal("70000"));
        wallet.setBalanceLocked(new BigDecimal("30000"));

        Transaction prevHold = new Transaction(wallet.getId(), "HOLD", new BigDecimal("30000"), listingId.toString());
        when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));
        when(transactionRepository.findByWalletIdAndReferenceId(any(), any())).thenReturn(List.of(prevHold));

        Wallet result = walletService.releaseBidFunds(userId, listingId, new BigDecimal("30000"));

        assertNotNull(result);
        assertEquals(new BigDecimal("100000"), result.getBalanceAvailable());
        assertEquals(BigDecimal.ZERO, result.getBalanceLocked());
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void releaseBidFunds_capsAtCurrentlyHeld() {
        UUID listingId = UUID.randomUUID();
        wallet.setBalanceAvailable(new BigDecimal("70000"));
        wallet.setBalanceLocked(new BigDecimal("30000"));

        Transaction prevHold = new Transaction(wallet.getId(), "HOLD", new BigDecimal("30000"), listingId.toString());
        when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));
        when(transactionRepository.findByWalletIdAndReferenceId(any(), any())).thenReturn(List.of(prevHold));

        Wallet result = walletService.releaseBidFunds(userId, listingId, new BigDecimal("99999"));

        assertNotNull(result);
        assertEquals(new BigDecimal("100000"), result.getBalanceAvailable());
        assertEquals(BigDecimal.ZERO, result.getBalanceLocked());
    }

    @Test
    void releaseBidFunds_nothingHeld_returnsWalletUnchanged() {
        UUID listingId = UUID.randomUUID();
        when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));
        when(transactionRepository.findByWalletIdAndReferenceId(any(), any())).thenReturn(List.of());

        Wallet result = walletService.releaseBidFunds(userId, listingId, new BigDecimal("30000"));

        assertNotNull(result);
        assertEquals(new BigDecimal("100000"), result.getBalanceAvailable());
        verify(walletRepository, never()).save(any());
    }

    @Test
    void releaseBidFunds_walletNotFound_returnsNull() {
        when(walletRepository.findByUserId(userId)).thenReturn(Optional.empty());
        assertNull(walletService.releaseBidFunds(userId, UUID.randomUUID(), new BigDecimal("10000")));
    }

    @Test
    void releaseBidFunds_invalidAmount_returnsNull() {
        assertNull(walletService.releaseBidFunds(userId, UUID.randomUUID(), BigDecimal.ZERO));
        assertNull(walletService.releaseBidFunds(userId, UUID.randomUUID(), null));
    }

    // === settlePayment ===

    @Test
    void settlePayment_success() {
        wallet.setBalanceAvailable(new BigDecimal("70000"));
        wallet.setBalanceLocked(new BigDecimal("30000"));

        when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Wallet result = walletService.settlePayment(userId, new BigDecimal("30000"), "bid-123");

        assertNotNull(result);
        assertEquals(new BigDecimal("70000"), result.getBalanceAvailable());
        assertEquals(BigDecimal.ZERO, result.getBalanceLocked());
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void settlePayment_insufficientLocked_returnsNull() {
        wallet.setBalanceLocked(new BigDecimal("10000"));
        when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));

        assertNull(walletService.settlePayment(userId, new BigDecimal("50000"), "bid-123"));
    }

    @Test
    void settlePayment_walletNotFound_returnsNull() {
        when(walletRepository.findByUserId(userId)).thenReturn(Optional.empty());
        assertNull(walletService.settlePayment(userId, new BigDecimal("10000"), "bid-123"));
    }

    // === withdraw ===

    @Test
    void withdraw_success() {
        when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Wallet result = walletService.withdraw(userId, new BigDecimal("40000"));

        assertNotNull(result);
        assertEquals(new BigDecimal("60000"), result.getBalanceAvailable());
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void withdraw_insufficientBalance_returnsNull() {
        when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));

        assertNull(walletService.withdraw(userId, new BigDecimal("200000")));
        verify(walletRepository, never()).save(any());
    }

    @Test
    void withdraw_walletNotFound_returnsNull() {
        when(walletRepository.findByUserId(userId)).thenReturn(Optional.empty());
        assertNull(walletService.withdraw(userId, new BigDecimal("10000")));
    }

    @Test
    void withdraw_invalidAmount_returnsNull() {
        assertNull(walletService.withdraw(userId, BigDecimal.ZERO));
        assertNull(walletService.withdraw(userId, null));
    }

    // === getTransactionHistory ===

    @Test
    void getTransactionHistory_success() {
        Transaction tx = new Transaction(wallet.getId(), "TOPUP", new BigDecimal("50000"), null);
        when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));
        when(transactionRepository.findByWalletIdOrderByCreatedAtDesc(any()))
                .thenReturn(List.of(tx));

        List<Transaction> result = walletService.getTransactionHistory(userId);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("TOPUP", result.get(0).getType());
    }

    @Test
    void getTransactionHistory_walletNotFound_returnsNull() {
        when(walletRepository.findByUserId(userId)).thenReturn(Optional.empty());
        assertNull(walletService.getTransactionHistory(userId));
    }

    @Test
    void getTransactionHistory_emptyList() {
        when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));
        when(transactionRepository.findByWalletIdOrderByCreatedAtDesc(any()))
                .thenReturn(List.of());

        List<Transaction> result = walletService.getTransactionHistory(userId);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}
