package com.example.bidmart.wallet.service;

import com.example.bidmart.common.event.BalanceHeldEvent;
import com.example.bidmart.common.event.BalanceIncomeEvent;
import com.example.bidmart.common.event.BalanceReleasedEvent;
import com.example.bidmart.common.event.BalanceSettledEvent;
import com.example.bidmart.common.event.BalanceTopUpEvent;
import com.example.bidmart.common.event.WithdrawEvent;
import com.example.bidmart.common.util.IdempotencyKeyGenerator;
import com.example.bidmart.wallet.dto.TopUpRequest;
import com.example.bidmart.wallet.dto.WithdrawRequest;
import com.example.bidmart.wallet.exception.InsufficientBalanceException;
import com.example.bidmart.wallet.exception.InvalidAmountException;
import com.example.bidmart.wallet.exception.InvalidRequestException;
import com.example.bidmart.wallet.exception.UnauthorizedException;
import com.example.bidmart.wallet.exception.WalletAlreadyExistsException;
import com.example.bidmart.wallet.exception.WalletNotFoundException;
import com.example.bidmart.wallet.model.PaymentMethod;
import com.example.bidmart.wallet.model.Transaction;
import com.example.bidmart.wallet.model.TransactionType;
import com.example.bidmart.wallet.model.Wallet;
import com.example.bidmart.wallet.repository.TransactionRepository;
import com.example.bidmart.wallet.repository.WalletRepository;
import com.example.bidmart.wallet.strategy.PaymentStrategy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class WalletServiceImpl implements WalletService {

    private final List<PaymentStrategy> paymentStrategies;
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final ApplicationEventPublisher eventPublisher;

    private static final BigDecimal MAX_TOPUP_AMOUNT = new BigDecimal("100000000");

    public WalletServiceImpl(
            List<PaymentStrategy> paymentStrategies,
            WalletRepository walletRepository,
            TransactionRepository transactionRepository,
            ApplicationEventPublisher eventPublisher
    ) {
        this.paymentStrategies = paymentStrategies;
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Functional interface representing an atomic operation on a Wallet.
     * Returns an Optional of Transaction to be saved, or Optional.empty() if no DB change is needed.
     */
    @FunctionalInterface
    private interface WalletMutation {
        Optional<Transaction> apply(Wallet wallet);
    }

    /**
     * Central execution point for all wallet mutations.
     * Handles idempotency check, pessimistic lock acquisition, wallet save, and transaction record creation.
     */
    private Wallet executeMutation(UUID userId, String idempotencyKey, WalletMutation mutation) {
        Optional<Wallet> existing = checkIdempotency(idempotencyKey, userId);
        if (existing.isPresent()) return existing.get();

        Wallet wallet = getWalletByUserIdWithLock(userId);
        Optional<Transaction> transactionOptional = mutation.apply(wallet);
        
        transactionOptional.ifPresent(transaction -> {
            walletRepository.save(wallet);
            transaction.setIdempotencyKey(idempotencyKey);
            transactionRepository.save(transaction);
        });
        
        return wallet;
    }

    @Override
    @Transactional
    public Wallet createWallet(UUID userId) {
        if (walletRepository.findByUserId(userId).isPresent()) {
            throw new WalletAlreadyExistsException("Wallet sudah ada untuk user ini.");
        }
        return walletRepository.save(new Wallet(userId));
    }

    @Override
    public Wallet getWalletByUserId(UUID userId) {
        return walletRepository.findByUserId(userId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet tidak ditemukan."));
    }

    @Override
    public List<Wallet> findAll() {
        return walletRepository.findAll();
    }

    @Override
    @Transactional
    public Wallet topUp(UUID userId, TopUpRequest request) {
        validateAmount(request.getAmount());
        validateTopUpAmount(request.getAmount());

        PaymentStrategy paymentStrategy = resolvePaymentStrategy(request.getMethod());
        paymentStrategy.validateDetails(request.getPaymentDetails());

        return executeMutation(userId, request.getIdempotencyKey(), wallet -> {
            wallet.setBalanceAvailable(wallet.getBalanceAvailable().add(request.getAmount()));
            
            eventPublisher.publishEvent(new BalanceTopUpEvent(userId, request.getAmount(), request.getMethod().toString()));
            
            String transactionNote = paymentStrategy.generateTransactionNote(TransactionType.TOPUP, request.getAmount(), request.getPaymentDetails());
            return Optional.of(new Transaction(wallet.getId(), TransactionType.TOPUP, request.getAmount(), transactionNote));
        });
    }

    @Override
    @Transactional
    public Wallet withdraw(UUID userId, WithdrawRequest request) {
        validateAmount(request.getAmount());

        PaymentStrategy paymentStrategy = resolvePaymentStrategy(request.getMethod());
        paymentStrategy.validateDetails(request.getPaymentDetails());

        return executeMutation(userId, request.getIdempotencyKey(), wallet -> {
            validateSufficientBalance(wallet, request.getAmount());

            wallet.setBalanceAvailable(wallet.getBalanceAvailable().subtract(request.getAmount()));
            
            String withdrawalDestination = request.getPaymentDetails() != null ? request.getPaymentDetails().toString() : request.getMethod().name();
            eventPublisher.publishEvent(new WithdrawEvent(userId, request.getAmount(), withdrawalDestination));
            
            String transactionNote = paymentStrategy.generateTransactionNote(TransactionType.WITHDRAWAL, request.getAmount(), request.getPaymentDetails());
            return Optional.of(new Transaction(wallet.getId(), TransactionType.WITHDRAWAL, request.getAmount(), transactionNote));
        });
    }

    @Override
    @Transactional
    public Wallet reserveBidFunds(UUID buyerId, UUID listingId, BigDecimal reserveTarget, String idempotencyKey) {
        validateAmount(reserveTarget);

        return executeMutation(buyerId, idempotencyKey, wallet -> {
            String referenceId = listingId.toString();
            BigDecimal currentlyHeld = calculateHeldForReference(wallet.getId(), referenceId);

            if (isReserveTargetMet(reserveTarget, currentlyHeld)) {
                return Optional.empty();
            }

            BigDecimal additionalLock = reserveTarget.subtract(currentlyHeld);
            validateSufficientBalance(wallet, additionalLock);

            lockBalance(wallet, additionalLock);

            eventPublisher.publishEvent(new BalanceHeldEvent(buyerId, additionalLock, listingId));
            return Optional.of(new Transaction(wallet.getId(), TransactionType.HOLD, additionalLock, referenceId));
        });
    }

    @Override
    @Transactional
    public Wallet releaseBidFunds(UUID buyerId, UUID listingId, BigDecimal releaseAmount, String idempotencyKey) {
        validateAmount(releaseAmount);

        return executeMutation(buyerId, idempotencyKey, wallet -> {
            String referenceId = listingId.toString();
            BigDecimal currentlyHeld = calculateHeldForReference(wallet.getId(), referenceId);
            
            if (isNoBalanceHeld(currentlyHeld)) {
                return Optional.empty();
            }

            BigDecimal actualRelease = currentlyHeld.min(releaseAmount);

            unlockBalance(wallet, actualRelease);

            eventPublisher.publishEvent(new BalanceReleasedEvent(buyerId, actualRelease, listingId));
            return Optional.of(new Transaction(wallet.getId(), TransactionType.REFUND, actualRelease, referenceId));
        });
    }

    @Override
    @Transactional
    public Wallet settlePayment(UUID userId, BigDecimal amount, String referenceId, String idempotencyKey) {
        validateAmount(amount);

        return executeMutation(userId, idempotencyKey, wallet -> {
            BigDecimal heldForReference = calculateHeldForReference(wallet.getId(), referenceId);
            validateSettlementAmount(amount, heldForReference);

            wallet.setBalanceLocked(wallet.getBalanceLocked().subtract(amount));

            eventPublisher.publishEvent(new BalanceSettledEvent(userId, amount, referenceId));
            return Optional.of(new Transaction(wallet.getId(), TransactionType.PAYMENT, amount, referenceId));
        });
    }

    @Override
    @Transactional
    public Wallet confirmDelivery(UUID sellerId, BigDecimal amount, String referenceId, String idempotencyKey) {
        validateAmount(amount);

        return executeMutation(sellerId, idempotencyKey, wallet -> {
            wallet.setBalanceAvailable(wallet.getBalanceAvailable().add(amount));

            eventPublisher.publishEvent(new BalanceIncomeEvent(sellerId, amount, referenceId));
            return Optional.of(new Transaction(wallet.getId(), TransactionType.INCOME, amount, referenceId));
        });
    }

    @Override
    @Transactional
    public void creditSellerAfterDelivery(UUID orderId, UUID listingId, UUID sellerId, BigDecimal amount) {
        String referenceId = listingId.toString();
        confirmDelivery(sellerId, amount, referenceId, "order-income-" + orderId);
    }

    @Override
    @Transactional
    public void refundBuyerForOrder(UUID orderId, UUID listingId, UUID buyerId, BigDecimal amount) {
        releaseBidFunds(buyerId, listingId, amount, "order-refund-" + orderId);
    }

    @Override
    public List<Transaction> getTransactionHistory(UUID userId) {
        if (!walletRepository.existsByUserId(userId)) {
            throw new WalletNotFoundException("Wallet tidak ditemukan.");
        }
        return transactionRepository.findByUserId(userId);
    }

    @Override
    public Transaction getTransactionById(UUID transactionId, UUID userId) {
        if (!walletRepository.existsByUserId(userId)) {
            throw new WalletNotFoundException("Wallet tidak ditemukan.");
        }
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new InvalidRequestException("Transaksi tidak ditemukan."));

        if (!walletRepository.existsByIdAndUserId(transaction.getWalletId(), userId)) {
            throw new UnauthorizedException("Anda tidak memiliki akses ke transaksi ini.");
        }

        return transaction;
    }

    @Override
    @Transactional
    public void releaseAllHoldsForUser(UUID userId) {
        Optional<Wallet> lockedWallet = walletRepository.findByUserIdWithLock(userId);
 
        if (lockedWallet.isEmpty()) {
            log.info("No wallet found for deactivated user: {}", userId);
            return;
        }
 
        Wallet wallet = lockedWallet.get();
 
        if (hasNoLockedBalance(wallet)) {
            log.info("No locked balance for user: {}", userId);
            return;
        }
 
        BigDecimal totalReleasedAmount = releaseAllHoldsFromWallet(wallet, userId);
 
        if (totalReleasedAmount.compareTo(BigDecimal.ZERO) > 0) {
            unlockBalance(wallet, totalReleasedAmount);
            walletRepository.save(wallet);
            log.info("Released total {} from locked balance for deactivated user: {}", totalReleasedAmount, userId);
        }
    }
 
    private boolean hasNoLockedBalance(Wallet wallet) {
        return wallet.getBalanceLocked().compareTo(BigDecimal.ZERO) <= 0;
    }
 
    private BigDecimal releaseAllHoldsFromWallet(Wallet wallet, UUID userId) {
        List<Transaction> holdTransactions = transactionRepository.findByWalletIdAndType(
                wallet.getId(), TransactionType.HOLD
        );
 
        Set<String> uniqueReferenceIds = holdTransactions.stream()
                .map(Transaction::getReferenceId)
                .collect(Collectors.toSet());
 
        BigDecimal totalReleasedAmount = BigDecimal.ZERO;
 
        for (String referenceId : uniqueReferenceIds) {
            BigDecimal netHeldAmount = calculateHeldForReference(wallet.getId(), referenceId);
 
            if (netHeldAmount.compareTo(BigDecimal.ZERO) > 0) {
                saveReleaseTransaction(wallet.getId(), userId, referenceId, netHeldAmount);
                totalReleasedAmount = totalReleasedAmount.add(netHeldAmount);
            }
        }
 
        return totalReleasedAmount;
    }
 
    private void saveReleaseTransaction(UUID walletId, UUID userId, String referenceId, BigDecimal releaseAmount) {
        String idempotencyKey = IdempotencyKeyGenerator.generate("DEACTIVATION_RELEASE", userId, referenceId);
 
        Transaction releaseTransaction = new Transaction(walletId, TransactionType.REFUND, releaseAmount, referenceId);
        releaseTransaction.setIdempotencyKey(idempotencyKey);
        transactionRepository.save(releaseTransaction);
 
        publishReleaseEvent(userId, releaseAmount, referenceId);
    }
 
    private void publishReleaseEvent(UUID userId, BigDecimal releaseAmount, String referenceId) {
        try {
            eventPublisher.publishEvent(
                    new BalanceReleasedEvent(userId, releaseAmount, UUID.fromString(referenceId))
            );
        } catch (IllegalArgumentException e) {
            log.warn("referenceId is not a valid UUID, release event not published: {}", referenceId);
        }
    }
 
    private Wallet getWalletByUserIdWithLock(UUID userId) {
        return walletRepository.findByUserIdWithLock(userId)
                .orElseGet(() -> {
                    log.info("Auto-creating wallet for user: {}", userId);
                    return walletRepository.save(new Wallet(userId));
                });
    }

    private PaymentStrategy resolvePaymentStrategy(PaymentMethod method) {
        if (method == null) {
            throw new InvalidRequestException("Metode pembayaran tidak boleh kosong.");
        }

        return paymentStrategies.stream()
                .filter(strategy -> strategy.supports(method))
                .findFirst()
                .orElseThrow(() -> new InvalidRequestException(
                        "Metode pembayaran '" + method + "' saat ini belum didukung oleh sistem."
                ));
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException("Jumlah harus lebih dari 0.");
        }
    }

    private void validateTopUpAmount(BigDecimal amount) {
        if (amount.compareTo(MAX_TOPUP_AMOUNT) > 0) {
            throw new InvalidAmountException("Jumlah top-up tidak boleh melebihi " + MAX_TOPUP_AMOUNT + ".");
        }
    }

    private BigDecimal calculateHeldForReference(UUID walletId, String referenceId) {
        BigDecimal netHeld = transactionRepository.calculateNetHeldAmount(walletId, referenceId);
        return netHeld.max(BigDecimal.ZERO);
    }

    private void validateSufficientBalance(Wallet wallet, BigDecimal requiredAmount) {
        if (wallet.getBalanceAvailable().compareTo(requiredAmount) < 0) {
            throw new InsufficientBalanceException(
                    "Saldo tidak mencukupi. Dibutuhkan: " + requiredAmount + ", tersedia: " + wallet.getBalanceAvailable());
        }
    }

    private void validateSettlementAmount(BigDecimal requestedAmount, BigDecimal heldAmount) {
        if (requestedAmount.compareTo(heldAmount) > 0) {
            throw new InvalidAmountException(
                    "Jumlah settlement melebihi saldo yang ditahan. Ditahan: " + heldAmount + ", diminta: " + requestedAmount);
        }
    }

    private boolean isNoBalanceHeld(BigDecimal heldAmount) {
        return heldAmount.compareTo(BigDecimal.ZERO) <= 0;
    }

    private boolean isReserveTargetMet(BigDecimal reserveTarget, BigDecimal currentlyHeld) {
        return reserveTarget.compareTo(currentlyHeld) <= 0;
    }

    private void lockBalance(Wallet wallet, BigDecimal amount) {
        wallet.setBalanceAvailable(wallet.getBalanceAvailable().subtract(amount));
        wallet.setBalanceLocked(wallet.getBalanceLocked().add(amount));
    }

    private void unlockBalance(Wallet wallet, BigDecimal amount) {
        wallet.setBalanceLocked(wallet.getBalanceLocked().subtract(amount));
        wallet.setBalanceAvailable(wallet.getBalanceAvailable().add(amount));
    }

    private Optional<Wallet> checkIdempotency(String idempotencyKey, UUID userId) {
        if (idempotencyKey == null) return Optional.empty();
        return transactionRepository.findByIdempotencyKey(idempotencyKey)
                .map(transaction -> getWalletByUserId(userId));
    }
}
