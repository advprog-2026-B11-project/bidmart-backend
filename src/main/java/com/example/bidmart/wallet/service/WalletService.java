package com.example.bidmart.wallet.service;

import com.example.bidmart.wallet.model.Wallet;
import com.example.bidmart.wallet.repository.WalletRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Service
public class WalletService {

    private final WalletRepository walletRepository;

    public WalletService(WalletRepository walletRepository) {
        this.walletRepository = walletRepository;
    }

    public Wallet createWallet(UUID userId) {
        Optional<Wallet> existing = walletRepository.findByUserId(userId);
        if (existing.isPresent()) {
            return null;
        }

        Wallet wallet = new Wallet(userId);
        return walletRepository.save(wallet);
    }

    public Wallet getWalletByUserId(UUID userId) {
        return walletRepository.findByUserId(userId).orElse(null);
    }

    public Wallet topUp(UUID userId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }

        Wallet wallet = walletRepository.findByUserId(userId).orElse(null);
        if (wallet == null) {
            return null;
        }

        wallet.setBalanceAvailable(wallet.getBalanceAvailable().add(amount));
        return walletRepository.save(wallet);
    }
}
