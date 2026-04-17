package com.example.bidmart.wallet.service;

import com.example.bidmart.wallet.model.Transaction;
import com.example.bidmart.wallet.model.Wallet;
import com.example.bidmart.wallet.repository.TransactionRepository;
import com.example.bidmart.wallet.repository.WalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class WalletService {

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;

    public WalletService(WalletRepository walletRepository, TransactionRepository transactionRepository) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
    }

    public Wallet createWallet(UUID userId) {
        if (walletRepository.findByUserId(userId).isPresent()) {
            return null;
        }

        Wallet wallet = new Wallet(userId);
        return walletRepository.save(wallet);
    }

    public Wallet getWalletByUserId(UUID userId) {
        return walletRepository.findByUserId(userId).orElse(null);
    }

    @Transactional
    public Wallet topUp(UUID userId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }

        Wallet wallet = walletRepository.findByUserId(userId).orElse(null);
        if (wallet == null) {
            return null;
        }

        wallet.setBalanceAvailable(wallet.getBalanceAvailable().add(amount));
        wallet = walletRepository.save(wallet);

        transactionRepository.save(new Transaction(wallet.getId(), "TOPUP", amount, null));

        return wallet;
    }

    public List<Wallet> findAll() {
        return walletRepository.findAll();
    }

    @Transactional
    public Wallet reserveBidFunds(UUID buyerId, UUID listingId, BigDecimal reserveTarget) {
        if (reserveTarget == null || reserveTarget.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }

        Wallet wallet = walletRepository.findByUserId(buyerId).orElse(null);
        if (wallet == null) {
            return null;
        }

        String refId = listingId.toString();

        BigDecimal currentlyHeld = calculateHeldForReference(wallet.getId(), refId);

        if (reserveTarget.compareTo(currentlyHeld) <= 0) {
            return wallet;
        }

        BigDecimal additionalLock = reserveTarget.subtract(currentlyHeld);

        if (wallet.getBalanceAvailable().compareTo(additionalLock) < 0) {
            return null;
        }

        wallet.setBalanceAvailable(wallet.getBalanceAvailable().subtract(additionalLock));
        wallet.setBalanceLocked(wallet.getBalanceLocked().add(additionalLock));
        wallet = walletRepository.save(wallet);

        transactionRepository.save(new Transaction(wallet.getId(), "HOLD", additionalLock, refId));

        return wallet;
    }

    @Transactional
    public Wallet releaseBidFunds(UUID buyerId, UUID listingId, BigDecimal releaseAmount) {
        if (releaseAmount == null || releaseAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }

        Wallet wallet = walletRepository.findByUserId(buyerId).orElse(null);
        if (wallet == null) {
            return null;
        }

        String refId = listingId.toString();

        BigDecimal currentlyHeld = calculateHeldForReference(wallet.getId(), refId);
        if (currentlyHeld.compareTo(BigDecimal.ZERO) <= 0) {
            return wallet;
        }

        BigDecimal actualRelease = currentlyHeld.min(releaseAmount);

        wallet.setBalanceLocked(wallet.getBalanceLocked().subtract(actualRelease));
        wallet.setBalanceAvailable(wallet.getBalanceAvailable().add(actualRelease));
        wallet = walletRepository.save(wallet);

        transactionRepository.save(new Transaction(wallet.getId(), "REFUND", actualRelease, refId));

        return wallet;
    }

    @Transactional
    public Wallet settlePayment(UUID userId, BigDecimal amount, String referenceId) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }

        Wallet wallet = walletRepository.findByUserId(userId).orElse(null);
        if (wallet == null) {
            return null;
        }

        if (wallet.getBalanceLocked().compareTo(amount) < 0) {
            return null;
        }

        wallet.setBalanceLocked(wallet.getBalanceLocked().subtract(amount));
        wallet = walletRepository.save(wallet);

        transactionRepository.save(new Transaction(wallet.getId(), "PAYMENT", amount, referenceId));

        return wallet;
    }

    @Transactional
    public Wallet withdraw(UUID userId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }

        Wallet wallet = walletRepository.findByUserId(userId).orElse(null);
        if (wallet == null) {
            return null;
        }

        if (wallet.getBalanceAvailable().compareTo(amount) < 0) {
            return null;
        }

        wallet.setBalanceAvailable(wallet.getBalanceAvailable().subtract(amount));
        wallet = walletRepository.save(wallet);

        transactionRepository.save(new Transaction(wallet.getId(), "WITHDRAWAL", amount, null));

        return wallet;
    }

    public List<Transaction> getTransactionHistory(UUID userId) {
        Wallet wallet = walletRepository.findByUserId(userId).orElse(null);
        if (wallet == null) {
            return null;
        }
        return transactionRepository.findByWalletIdOrderByCreatedAtDesc(wallet.getId());
    }

    @Transactional
    public Wallet confirmDelivery(UUID sellerId, BigDecimal amount, String referenceId) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }

        Wallet sellerWallet = walletRepository.findByUserId(sellerId).orElse(null);
        if (sellerWallet == null) {
            return null;
        }

        sellerWallet.setBalanceAvailable(sellerWallet.getBalanceAvailable().add(amount));
        sellerWallet = walletRepository.save(sellerWallet);

        transactionRepository.save(new Transaction(sellerWallet.getId(), "INCOME", amount, referenceId));

        return sellerWallet;
    }

    private BigDecimal calculateHeldForReference(UUID walletId, String referenceId) {
        List<Transaction> txs = transactionRepository.findByWalletIdAndReferenceId(walletId, referenceId);

        BigDecimal held = BigDecimal.ZERO;
        for (Transaction tx : txs) {
            if ("HOLD".equals(tx.getType())) {
                held = held.add(tx.getAmount());
            } else if ("REFUND".equals(tx.getType()) || "PAYMENT".equals(tx.getType())) {
                held = held.subtract(tx.getAmount());
            }
        }

        return held.max(BigDecimal.ZERO);
    }
}
