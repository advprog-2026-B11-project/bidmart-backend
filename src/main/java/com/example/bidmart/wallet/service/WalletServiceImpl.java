package com.example.bidmart.wallet.service;

import com.example.bidmart.common.event.*;
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

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;
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
        Optional<Wallet> existing = checkIdempotency(request.getIdempotencyKey(), userId);
        if (existing.isPresent()) return existing.get();

        validateAmount(request.getAmount());
        validateTopUpAmount(request.getAmount());

        PaymentStrategy strategy = resolvePaymentStrategy(request.getMethod());
        strategy.validateDetails(request.getPaymentDetails());

        Wallet wallet = getWalletByUserIdWithLock(userId);
        wallet.setBalanceAvailable(wallet.getBalanceAvailable().add(request.getAmount()));
        wallet = walletRepository.save(wallet);

        String note = strategy.generateTransactionNote("TOPUP", request.getAmount(), request.getPaymentDetails());
        Transaction tx = new Transaction(wallet.getId(), TransactionType.TOPUP, request.getAmount(), note);
        tx.setIdempotencyKey(request.getIdempotencyKey());
        transactionRepository.save(tx);

        eventPublisher.publishEvent(
                new BalanceTopUpEvent(userId, request.getAmount(), request.getMethod().toString())
        );

        return wallet;
    }

    @Override
    @Transactional
    public Wallet withdraw(UUID userId, WithdrawRequest request) {
        Optional<Wallet> existing = checkIdempotency(request.getIdempotencyKey(), userId);
        if (existing.isPresent()) return existing.get();

        validateAmount(request.getAmount());

        PaymentStrategy strategy = resolvePaymentStrategy(request.getMethod());
        strategy.validateDetails(request.getPaymentDetails());

        Wallet wallet = getWalletByUserIdWithLock(userId);
        if (wallet.getBalanceAvailable().compareTo(request.getAmount()) < 0) {
            throw new InsufficientBalanceException(
                    "Saldo tidak mencukupi. Dibutuhkan: " + request.getAmount() + ", tersedia: " + wallet.getBalanceAvailable());
        }

        wallet.setBalanceAvailable(wallet.getBalanceAvailable().subtract(request.getAmount()));
        wallet = walletRepository.save(wallet);

        String note = strategy.generateTransactionNote("WITHDRAW", request.getAmount(), request.getPaymentDetails());
        Transaction tx = new Transaction(wallet.getId(), TransactionType.WITHDRAWAL, request.getAmount(), note);
        tx.setIdempotencyKey(request.getIdempotencyKey());
        transactionRepository.save(tx);

        String bankName = request.getPaymentDetails() != null ? request.getPaymentDetails().toString() : "Bank";
        eventPublisher.publishEvent(new WithdrawEvent(userId, request.getAmount(), bankName));

        return wallet;
    }

    @Override
    public Wallet reserveBidFunds(UUID buyerId, UUID listingId, BigDecimal reserveTarget) {
        String key = generateDeterministicKey("HOLD", buyerId, listingId.toString(), reserveTarget);
        return reserveBidFunds(buyerId, listingId, reserveTarget, key);
    }

    @Override
    @Transactional
    public Wallet reserveBidFunds(UUID buyerId, UUID listingId, BigDecimal reserveTarget, String idempotencyKey) {
        Optional<Wallet> existing = checkIdempotency(idempotencyKey, buyerId);
        if (existing.isPresent()) return existing.get();

        validateAmount(reserveTarget);

        Wallet wallet = getWalletByUserIdWithLock(buyerId);
        String refId = listingId.toString();

        BigDecimal currentlyHeld = calculateHeldForReference(wallet.getId(), refId);

        if (reserveTarget.compareTo(currentlyHeld) <= 0) {
            return wallet;
        }

        BigDecimal additionalLock = reserveTarget.subtract(currentlyHeld);

        if (wallet.getBalanceAvailable().compareTo(additionalLock) < 0) {
            throw new InsufficientBalanceException(
                    "Saldo tidak mencukupi. Dibutuhkan: " + additionalLock + ", tersedia: " + wallet.getBalanceAvailable());
        }

        wallet.setBalanceAvailable(wallet.getBalanceAvailable().subtract(additionalLock));
        wallet.setBalanceLocked(wallet.getBalanceLocked().add(additionalLock));
        wallet = walletRepository.save(wallet);

        Transaction tx = new Transaction(wallet.getId(), TransactionType.HOLD, additionalLock, refId);
        tx.setIdempotencyKey(idempotencyKey);
        transactionRepository.save(tx);

        eventPublisher.publishEvent(new BalanceHeldEvent(buyerId, additionalLock, listingId));

        return wallet;
    }

    @Override
    public Wallet releaseBidFunds(UUID buyerId, UUID listingId, BigDecimal releaseAmount) {
        String key = generateDeterministicKey("RELEASE", buyerId, listingId.toString(), releaseAmount);
        return releaseBidFunds(buyerId, listingId, releaseAmount, key);
    }

    @Override
    @Transactional
    public Wallet releaseBidFunds(UUID buyerId, UUID listingId, BigDecimal releaseAmount, String idempotencyKey) {
        Optional<Wallet> existing = checkIdempotency(idempotencyKey, buyerId);
        if (existing.isPresent()) return existing.get();

        validateAmount(releaseAmount);

        Wallet wallet = getWalletByUserIdWithLock(buyerId);
        String refId = listingId.toString();

        BigDecimal currentlyHeld = calculateHeldForReference(wallet.getId(), refId);
        if (currentlyHeld.compareTo(BigDecimal.ZERO) <= 0) {
            return wallet;
        }

        BigDecimal actualRelease = currentlyHeld.min(releaseAmount);

        wallet.setBalanceLocked(wallet.getBalanceLocked().subtract(actualRelease));
        wallet.setBalanceAvailable(wallet.getBalanceAvailable().add(actualRelease));
        wallet = walletRepository.save(wallet);

        Transaction tx = new Transaction(wallet.getId(), TransactionType.REFUND, actualRelease, refId);
        tx.setIdempotencyKey(idempotencyKey);
        transactionRepository.save(tx);

        eventPublisher.publishEvent(new BalanceReleasedEvent(buyerId, actualRelease, listingId));

        return wallet;
    }

    @Override
    public Wallet settlePayment(UUID userId, BigDecimal amount, String referenceId) {
        String key = generateDeterministicKey("SETTLE", userId, referenceId, amount);
        return settlePayment(userId, amount, referenceId, key);
    }

    @Override
    @Transactional
    public Wallet settlePayment(UUID userId, BigDecimal amount, String referenceId, String idempotencyKey) {
        Optional<Wallet> existing = checkIdempotency(idempotencyKey, userId);
        if (existing.isPresent()) return existing.get();

        validateAmount(amount);

        Wallet wallet = getWalletByUserIdWithLock(userId);

        BigDecimal heldForReference = calculateHeldForReference(wallet.getId(), referenceId);
        if (amount.compareTo(heldForReference) > 0) {
            throw new InvalidAmountException(
                    "Jumlah settlement melebihi saldo yang ditahan. Ditahan: " + heldForReference + ", diminta: " + amount);
        }

        wallet.setBalanceLocked(wallet.getBalanceLocked().subtract(amount));
        wallet = walletRepository.save(wallet);

        Transaction tx = new Transaction(wallet.getId(), TransactionType.PAYMENT, amount, referenceId);
        tx.setIdempotencyKey(idempotencyKey);
        transactionRepository.save(tx);

        eventPublisher.publishEvent(new BalanceSettledEvent(userId, amount, referenceId));

        return wallet;
    }

    @Override
    public Wallet confirmDelivery(UUID sellerId, BigDecimal amount, String referenceId) {
        String key = generateDeterministicKey("DELIVERY", sellerId, referenceId, amount);
        return confirmDelivery(sellerId, amount, referenceId, key);
    }

    @Override
    @Transactional
    public Wallet confirmDelivery(UUID sellerId, BigDecimal amount, String referenceId, String idempotencyKey) {
        Optional<Wallet> existing = checkIdempotency(idempotencyKey, sellerId);
        if (existing.isPresent()) return existing.get();

        validateAmount(amount);

        Wallet sellerWallet = getWalletByUserIdWithLock(sellerId);
        sellerWallet.setBalanceAvailable(sellerWallet.getBalanceAvailable().add(amount));
        sellerWallet = walletRepository.save(sellerWallet);

        Transaction tx = new Transaction(sellerWallet.getId(), TransactionType.INCOME, amount, referenceId);
        tx.setIdempotencyKey(idempotencyKey);
        transactionRepository.save(tx);

        eventPublisher.publishEvent(new BalanceIncomeEvent(sellerId, amount, referenceId));

        return sellerWallet;
    }

    @Override
    @Transactional
    public void completeOrderPayment(UUID orderId, UUID listingId, UUID buyerId, UUID sellerId, BigDecimal amount) {
        String referenceId = listingId.toString();
        settlePayment(buyerId, amount, referenceId, "order-settle-" + orderId);
        confirmDelivery(sellerId, amount, referenceId, "order-income-" + orderId);
    }

    @Override
    @Transactional
    public void refundOrderPayment(UUID orderId, UUID listingId, UUID buyerId, BigDecimal amount) {
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
        Optional<Wallet> walletOpt = walletRepository.findByUserIdWithLock(userId);
        if (walletOpt.isEmpty()) {
            log.info("No wallet found for deactivated user: {}", userId);
            return;
        }

        Wallet wallet = walletOpt.get();
        if (wallet.getBalanceLocked().compareTo(BigDecimal.ZERO) <= 0) {
            log.info("No locked balance for user: {}", userId);
            return;
        }

        List<Transaction> holdTxs = transactionRepository.findByWalletIdAndType(wallet.getId(), TransactionType.HOLD);

        Set<String> referenceIds = holdTxs.stream()
                .map(Transaction::getReferenceId)
                .collect(Collectors.toSet());

        BigDecimal totalReleased = BigDecimal.ZERO;

        for (String refId : referenceIds) {
            BigDecimal netHeld = calculateHeldForReference(wallet.getId(), refId);
            if (netHeld.compareTo(BigDecimal.ZERO) > 0) {
                Transaction refundTx = new Transaction(wallet.getId(), TransactionType.REFUND, netHeld, refId);
                transactionRepository.save(refundTx);
                totalReleased = totalReleased.add(netHeld);

                try {
                    eventPublisher.publishEvent(
                            new BalanceReleasedEvent(userId, netHeld, UUID.fromString(refId))
                    );
                } catch (IllegalArgumentException e) {
                    log.warn("Could not parse referenceId as UUID for event: {}", refId);
                }
            }
        }

        if (totalReleased.compareTo(BigDecimal.ZERO) > 0) {
            wallet.setBalanceLocked(wallet.getBalanceLocked().subtract(totalReleased));
            wallet.setBalanceAvailable(wallet.getBalanceAvailable().add(totalReleased));
            walletRepository.save(wallet);
            log.info("Released total {} from locked balance for deactivated user: {}", totalReleased, userId);
        }
    }
    
    private Wallet getWalletByUserIdWithLock(UUID userId) {
        return walletRepository.findByUserIdWithLock(userId)
                .orElseGet(() -> {
                    Wallet newWallet = new Wallet(userId);
                    return walletRepository.save(newWallet);
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

    private Optional<Wallet> checkIdempotency(String idempotencyKey, UUID userId) {
        if (idempotencyKey == null) return Optional.empty();
        return transactionRepository.findByIdempotencyKey(idempotencyKey)
                .map(tx -> getWalletByUserId(userId));
    }

    private String generateDeterministicKey(String operation, UUID userId, String referenceId, BigDecimal amount) {
        String raw = operation + ":" + userId + ":" + referenceId + ":" + amount.toPlainString();
        return UUID.nameUUIDFromBytes(raw.getBytes(StandardCharsets.UTF_8)).toString();
    }
}
