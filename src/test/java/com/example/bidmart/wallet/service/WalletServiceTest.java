package com.example.bidmart.wallet.service;

import com.example.bidmart.wallet.dto.TopUpRequest;
import com.example.bidmart.wallet.dto.WithdrawRequest;
import com.example.bidmart.wallet.exception.*;
import com.example.bidmart.wallet.model.PaymentMethod;
import com.example.bidmart.wallet.model.Transaction;
import com.example.bidmart.wallet.model.TransactionType;
import com.example.bidmart.wallet.model.Wallet;
import com.example.bidmart.wallet.repository.TransactionRepository;
import com.example.bidmart.wallet.repository.WalletRepository;
import com.example.bidmart.wallet.strategy.PaymentStrategy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock private WalletRepository walletRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private PaymentStrategy paymentStrategy;
    @Mock private ApplicationEventPublisher eventPublisher;

    private WalletServiceImpl walletService;
    private UUID userId;
    private UUID listingId;
    private Wallet wallet;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        listingId = UUID.randomUUID();
        wallet = new Wallet(userId);
        wallet.setId(UUID.randomUUID());
        walletService = new WalletServiceImpl(
                List.of(paymentStrategy), walletRepository, transactionRepository, eventPublisher
        );
    }

    private TopUpRequest topUpReq(BigDecimal amt) {
        return new TopUpRequest(amt, PaymentMethod.BANK,
                Map.of("bankName", "BCA", "accountNumber", "123"), "IDEMP-" + UUID.randomUUID());
    }

    private WithdrawRequest withdrawReq(BigDecimal amt) {
        return new WithdrawRequest(amt, PaymentMethod.BANK,
                Map.of("bankName", "BCA", "accountNumber", "123"), "IDEMP-" + UUID.randomUUID());
    }

    @Nested class CreateWallet {
        @Test void success() {
            when(walletRepository.findByUserId(userId)).thenReturn(Optional.empty());
            when(walletRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            Wallet w = walletService.createWallet(userId);
            assertEquals(userId, w.getUserId());
            assertEquals(BigDecimal.ZERO, w.getBalanceAvailable());
        }
        @Test void alreadyExists_throws() {
            when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));
            assertThrows(WalletAlreadyExistsException.class, () -> walletService.createWallet(userId));
            verify(walletRepository, never()).save(any());
        }
    }

    @Nested class GetWallet {
        @Test void found() {
            when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));
            assertEquals(userId, walletService.getWalletByUserId(userId).getUserId());
        }
        @Test void notFound_throws() {
            when(walletRepository.findByUserId(userId)).thenReturn(Optional.empty());
            assertThrows(WalletNotFoundException.class, () -> walletService.getWalletByUserId(userId));
        }
    }

    @Nested class FindAll {
        @Test void returnsList() {
            when(walletRepository.findAll()).thenReturn(List.of(wallet));
            assertEquals(1, walletService.findAll().size());
        }
    }

    @Nested class TopUp {
        @BeforeEach void stubStrategy() {
            lenient().when(paymentStrategy.supports(PaymentMethod.BANK)).thenReturn(true);
            lenient().when(paymentStrategy.generateTransactionNote(any(), any(), any())).thenReturn("note");
        }

        @Test void success_addsBalance_publishesEvent() {
            TopUpRequest req = topUpReq(new BigDecimal("50000"));
            when(walletRepository.findByUserIdWithLock(userId)).thenReturn(Optional.of(wallet));
            when(walletRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            Wallet w = walletService.topUp(userId, req);
            assertEquals(new BigDecimal("50000"), w.getBalanceAvailable());
            verify(transactionRepository).save(argThat(tx -> tx.getType() == TransactionType.TOPUP));
            verify(eventPublisher).publishEvent(any(Object.class));
        }

        @Test void idempotent_skipsIfKeyExists() {
            TopUpRequest req = topUpReq(new BigDecimal("50000"));
            when(transactionRepository.findByIdempotencyKey(req.getIdempotencyKey()))
                    .thenReturn(Optional.of(new Transaction()));
            when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));

            walletService.topUp(userId, req);
            verify(walletRepository, never()).save(any());
            verify(eventPublisher, never()).publishEvent(any(Object.class));
        }

        @Test void zeroAmount_throws() {
            assertThrows(InvalidAmountException.class, () -> walletService.topUp(userId, topUpReq(BigDecimal.ZERO)));
        }
        @Test void negativeAmount_throws() {
            assertThrows(InvalidAmountException.class, () -> walletService.topUp(userId, topUpReq(new BigDecimal("-1"))));
        }
        @Test void nullAmount_throws() {
            assertThrows(InvalidAmountException.class, () -> walletService.topUp(userId, topUpReq(null)));
        }
        @Test void exceedsMax_throws() {
            assertThrows(InvalidAmountException.class, () -> walletService.topUp(userId, topUpReq(new BigDecimal("200000000"))));
        }
        @Test void walletNotFound_throws() {
            when(walletRepository.findByUserIdWithLock(userId)).thenReturn(Optional.empty());
            assertThrows(WalletNotFoundException.class, () -> walletService.topUp(userId, topUpReq(new BigDecimal("1000"))));
        }
        @Test void nullPaymentMethod_throws() {
            TopUpRequest req = new TopUpRequest(new BigDecimal("1000"), null, Map.of(), "k");
            assertThrows(InvalidRequestException.class, () -> walletService.topUp(userId, req));
        }
        @Test void unsupportedMethod_throws() {
            when(paymentStrategy.supports(PaymentMethod.GOPAY)).thenReturn(false);
            TopUpRequest req = new TopUpRequest(new BigDecimal("1000"), PaymentMethod.GOPAY, Map.of(), "k");
            assertThrows(InvalidRequestException.class, () -> walletService.topUp(userId, req));
        }
    }

    @Nested class Withdraw {
        @BeforeEach void stubStrategy() {
            lenient().when(paymentStrategy.supports(PaymentMethod.BANK)).thenReturn(true);
            lenient().when(paymentStrategy.generateTransactionNote(any(), any(), any())).thenReturn("note");
        }

        @Test void success_subtractsBalance_publishesEvent() {
            wallet.setBalanceAvailable(new BigDecimal("100000"));
            WithdrawRequest req = withdrawReq(new BigDecimal("30000"));
            when(walletRepository.findByUserIdWithLock(userId)).thenReturn(Optional.of(wallet));
            when(walletRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            Wallet w = walletService.withdraw(userId, req);
            assertEquals(new BigDecimal("70000"), w.getBalanceAvailable());
            verify(transactionRepository).save(argThat(tx -> tx.getType() == TransactionType.WITHDRAWAL));
            verify(eventPublisher).publishEvent(any(Object.class));
        }
        @Test void insufficientBalance_throws() {
            wallet.setBalanceAvailable(new BigDecimal("1000"));
            WithdrawRequest req = withdrawReq(new BigDecimal("5000"));
            when(walletRepository.findByUserIdWithLock(userId)).thenReturn(Optional.of(wallet));
            assertThrows(InsufficientBalanceException.class, () -> walletService.withdraw(userId, req));
        }
        @Test void invalidDetails_throws() {
            WithdrawRequest req = new WithdrawRequest(new BigDecimal("1000"), PaymentMethod.BANK, new HashMap<>(), "k");
            doThrow(new InvalidRequestException("bad")).when(paymentStrategy).validateDetails(any());
            assertThrows(InvalidRequestException.class, () -> walletService.withdraw(userId, req));
            verify(walletRepository, never()).findByUserIdWithLock(any());
        }
        @Test void idempotent() {
            WithdrawRequest req = withdrawReq(new BigDecimal("1000"));
            when(transactionRepository.findByIdempotencyKey(req.getIdempotencyKey()))
                    .thenReturn(Optional.of(new Transaction()));
            when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));
            walletService.withdraw(userId, req);
            verify(walletRepository, never()).save(any());
        }
    }

    @Nested class ReserveBidFunds {
        @Test void success_newHold() {
            wallet.setBalanceAvailable(new BigDecimal("100000"));
            when(walletRepository.findByUserIdWithLock(userId)).thenReturn(Optional.of(wallet));
            when(transactionRepository.findByWalletIdAndReferenceId(any(), any())).thenReturn(Collections.emptyList());
            when(walletRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            Wallet w = walletService.reserveBidFunds(userId, listingId, new BigDecimal("50000"), "k1");
            assertEquals(new BigDecimal("50000"), w.getBalanceAvailable());
            assertEquals(new BigDecimal("50000"), w.getBalanceLocked());
            verify(eventPublisher).publishEvent(any(Object.class));
        }
        @Test void alreadyHeldEnough_noOp() {
            wallet.setBalanceAvailable(new BigDecimal("100000"));
            wallet.setBalanceLocked(new BigDecimal("50000"));
            Transaction holdTx = new Transaction(wallet.getId(), TransactionType.HOLD, new BigDecimal("50000"), listingId.toString());
            when(walletRepository.findByUserIdWithLock(userId)).thenReturn(Optional.of(wallet));
            when(transactionRepository.findByWalletIdAndReferenceId(any(), any())).thenReturn(List.of(holdTx));

            Wallet w = walletService.reserveBidFunds(userId, listingId, new BigDecimal("50000"), "k1");
            assertEquals(new BigDecimal("100000"), w.getBalanceAvailable());
            verify(walletRepository, never()).save(any());
            verify(eventPublisher, never()).publishEvent(any(Object.class));
        }
        @Test void insufficientBalance_throws() {
            wallet.setBalanceAvailable(new BigDecimal("10000"));
            when(walletRepository.findByUserIdWithLock(userId)).thenReturn(Optional.of(wallet));
            when(transactionRepository.findByWalletIdAndReferenceId(any(), any())).thenReturn(Collections.emptyList());

            assertThrows(InsufficientBalanceException.class,
                    () -> walletService.reserveBidFunds(userId, listingId, new BigDecimal("50000"), "k1"));
        }
        @Test void overloadWithoutIdempotencyKey() {
            wallet.setBalanceAvailable(new BigDecimal("100000"));
            when(walletRepository.findByUserIdWithLock(userId)).thenReturn(Optional.of(wallet));
            when(transactionRepository.findByWalletIdAndReferenceId(any(), any())).thenReturn(Collections.emptyList());
            when(walletRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            Wallet w = walletService.reserveBidFunds(userId, listingId, new BigDecimal("30000"));
            assertEquals(new BigDecimal("70000"), w.getBalanceAvailable());
        }
    }

    @Nested class ReleaseBidFunds {
        @Test void success_releasesHeld() {
            wallet.setBalanceLocked(new BigDecimal("50000"));
            Transaction holdTx = new Transaction(wallet.getId(), TransactionType.HOLD, new BigDecimal("50000"), listingId.toString());
            when(walletRepository.findByUserIdWithLock(userId)).thenReturn(Optional.of(wallet));
            when(transactionRepository.findByWalletIdAndReferenceId(any(), any())).thenReturn(List.of(holdTx));
            when(walletRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            Wallet w = walletService.releaseBidFunds(userId, listingId, new BigDecimal("30000"), "k1");
            assertEquals(new BigDecimal("20000"), w.getBalanceLocked());
            assertEquals(new BigDecimal("30000"), w.getBalanceAvailable());
            verify(eventPublisher).publishEvent(any(Object.class));
        }
        @Test void nothingHeld_noOp() {
            wallet.setBalanceLocked(BigDecimal.ZERO);
            when(walletRepository.findByUserIdWithLock(userId)).thenReturn(Optional.of(wallet));
            when(transactionRepository.findByWalletIdAndReferenceId(any(), any())).thenReturn(Collections.emptyList());

            Wallet w = walletService.releaseBidFunds(userId, listingId, new BigDecimal("30000"), "k1");
            assertEquals(BigDecimal.ZERO, w.getBalanceLocked());
            verify(walletRepository, never()).save(any());
        }
        @Test void capsToHeldAmount() {
            wallet.setBalanceLocked(new BigDecimal("20000"));
            Transaction holdTx = new Transaction(wallet.getId(), TransactionType.HOLD, new BigDecimal("20000"), listingId.toString());
            when(walletRepository.findByUserIdWithLock(userId)).thenReturn(Optional.of(wallet));
            when(transactionRepository.findByWalletIdAndReferenceId(any(), any())).thenReturn(List.of(holdTx));
            when(walletRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            Wallet w = walletService.releaseBidFunds(userId, listingId, new BigDecimal("99999"), "k1");
            assertEquals(BigDecimal.ZERO, w.getBalanceLocked());
            assertEquals(new BigDecimal("20000"), w.getBalanceAvailable());
        }
    }

    @Nested class SettlePayment {
        @Test void success() {
            wallet.setBalanceLocked(new BigDecimal("50000"));
            Transaction holdTx = new Transaction(wallet.getId(), TransactionType.HOLD, new BigDecimal("50000"), "REF");
            when(walletRepository.findByUserIdWithLock(userId)).thenReturn(Optional.of(wallet));
            when(transactionRepository.findByWalletIdAndReferenceId(any(), eq("REF"))).thenReturn(List.of(holdTx));
            when(walletRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            Wallet w = walletService.settlePayment(userId, new BigDecimal("50000"), "REF", "k1");
            assertEquals(BigDecimal.ZERO, w.getBalanceLocked());
            verify(transactionRepository).save(argThat(tx -> tx.getType() == TransactionType.PAYMENT));
            verify(eventPublisher).publishEvent(any(Object.class));
        }
        @Test void exceedsHeld_throws() {
            wallet.setBalanceLocked(new BigDecimal("30000"));
            Transaction holdTx = new Transaction(wallet.getId(), TransactionType.HOLD, new BigDecimal("30000"), "REF");
            when(walletRepository.findByUserIdWithLock(userId)).thenReturn(Optional.of(wallet));
            when(transactionRepository.findByWalletIdAndReferenceId(any(), eq("REF"))).thenReturn(List.of(holdTx));

            assertThrows(InvalidAmountException.class,
                    () -> walletService.settlePayment(userId, new BigDecimal("50000"), "REF", "k1"));
        }
        @Test void overloadWithoutKey() {
            wallet.setBalanceLocked(new BigDecimal("50000"));
            Transaction holdTx = new Transaction(wallet.getId(), TransactionType.HOLD, new BigDecimal("50000"), "REF");
            when(walletRepository.findByUserIdWithLock(userId)).thenReturn(Optional.of(wallet));
            when(transactionRepository.findByWalletIdAndReferenceId(any(), eq("REF"))).thenReturn(List.of(holdTx));
            when(walletRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            assertNotNull(walletService.settlePayment(userId, new BigDecimal("50000"), "REF"));
        }
    }

    @Nested class ConfirmDelivery {
        @Test void success_addsIncome() {
            when(walletRepository.findByUserIdWithLock(userId)).thenReturn(Optional.of(wallet));
            when(walletRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            Wallet w = walletService.confirmDelivery(userId, new BigDecimal("75000"), "REF", "k1");
            assertEquals(new BigDecimal("75000"), w.getBalanceAvailable());
            verify(transactionRepository).save(argThat(tx -> tx.getType() == TransactionType.INCOME));
            verify(eventPublisher).publishEvent(any(Object.class));
        }
        @Test void invalidAmount_throws() {
            assertThrows(InvalidAmountException.class,
                    () -> walletService.confirmDelivery(userId, BigDecimal.ZERO, "REF", "k1"));
        }
    }

    @Nested class TransactionHistory {
        @Test void success() {
            Transaction tx = new Transaction(wallet.getId(), TransactionType.TOPUP, new BigDecimal("50000"), "n");
            when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));
            when(transactionRepository.findByWalletIdOrderByCreatedAtDesc(wallet.getId())).thenReturn(List.of(tx));

            assertEquals(1, walletService.getTransactionHistory(userId).size());
        }
        @Test void walletNotFound_throws() {
            when(walletRepository.findByUserId(userId)).thenReturn(Optional.empty());
            assertThrows(WalletNotFoundException.class, () -> walletService.getTransactionHistory(userId));
        }
    }

    @Nested class GetTransactionById {
        @Test void success() {
            UUID txId = UUID.randomUUID();
            Transaction tx = new Transaction(wallet.getId(), TransactionType.TOPUP, new BigDecimal("50000"), "n");
            tx.setId(txId);
            when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));
            when(transactionRepository.findById(txId)).thenReturn(Optional.of(tx));

            assertEquals(txId, walletService.getTransactionById(txId, userId).getId());
        }
        @Test void notFound_throws() {
            UUID txId = UUID.randomUUID();
            when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));
            when(transactionRepository.findById(txId)).thenReturn(Optional.empty());
            assertThrows(InvalidRequestException.class, () -> walletService.getTransactionById(txId, userId));
        }
        @Test void differentWallet_throws() {
            UUID txId = UUID.randomUUID();
            Transaction tx = new Transaction(UUID.randomUUID(), TransactionType.TOPUP, new BigDecimal("50000"), "n");
            tx.setId(txId);
            when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));
            when(transactionRepository.findById(txId)).thenReturn(Optional.of(tx));
            assertThrows(UnauthorizedException.class, () -> walletService.getTransactionById(txId, userId));
        }
    }

    @Nested class ReleaseAllHoldsForUser {
        @Test void releasesAllActiveHolds() {
            wallet.setBalanceLocked(new BigDecimal("80000"));
            UUID listing1 = UUID.randomUUID();
            UUID listing2 = UUID.randomUUID();

            Transaction h1 = new Transaction(wallet.getId(), TransactionType.HOLD, new BigDecimal("50000"), listing1.toString());
            Transaction h2 = new Transaction(wallet.getId(), TransactionType.HOLD, new BigDecimal("30000"), listing2.toString());

            when(walletRepository.findByUserIdWithLock(userId)).thenReturn(Optional.of(wallet));
            when(transactionRepository.findByWalletIdAndType(wallet.getId(), TransactionType.HOLD)).thenReturn(List.of(h1, h2));
            when(transactionRepository.findByWalletIdAndReferenceId(wallet.getId(), listing1.toString())).thenReturn(List.of(h1));
            when(transactionRepository.findByWalletIdAndReferenceId(wallet.getId(), listing2.toString())).thenReturn(List.of(h2));
            when(walletRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            walletService.releaseAllHoldsForUser(userId);

            verify(transactionRepository, times(2)).save(argThat(tx -> tx.getType() == TransactionType.REFUND));
            verify(walletRepository).save(argThat(w -> w.getBalanceLocked().compareTo(BigDecimal.ZERO) == 0));
        }
        @Test void noWallet_doesNothing() {
            when(walletRepository.findByUserIdWithLock(userId)).thenReturn(Optional.empty());
            walletService.releaseAllHoldsForUser(userId);
            verify(transactionRepository, never()).save(any());
        }
        @Test void noLockedBalance_doesNothing() {
            wallet.setBalanceLocked(BigDecimal.ZERO);
            when(walletRepository.findByUserIdWithLock(userId)).thenReturn(Optional.of(wallet));
            walletService.releaseAllHoldsForUser(userId);
            verify(transactionRepository, never()).findByWalletIdAndType(any(), any());
        }
    }
}
