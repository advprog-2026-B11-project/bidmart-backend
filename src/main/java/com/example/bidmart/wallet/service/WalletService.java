package com.example.bidmart.wallet.service;

import com.example.bidmart.wallet.dto.TopUpRequest;
import com.example.bidmart.wallet.dto.WithdrawRequest;
import com.example.bidmart.wallet.model.Transaction;
import com.example.bidmart.wallet.model.Wallet;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface WalletService {
    Wallet createWallet(UUID userId);
    Wallet getWalletByUserId(UUID userId);
    List<Wallet> findAll();

    Wallet topUp(UUID userId, TopUpRequest request);
    Wallet withdraw(UUID userId, WithdrawRequest request);

    // Bidding
    Wallet reserveBidFunds(UUID buyerId, UUID listingId, BigDecimal reserveTarget);
    Wallet reserveBidFunds(UUID buyerId, UUID listingId, BigDecimal reserveTarget, String idempotencyKey);
    Wallet releaseBidFunds(UUID buyerId, UUID listingId, BigDecimal releaseAmount);
    Wallet releaseBidFunds(UUID buyerId, UUID listingId, BigDecimal releaseAmount, String idempotencyKey);

    // Settlement & Audit
    Wallet settlePayment(UUID userId, BigDecimal amount, String referenceId);
    Wallet settlePayment(UUID userId, BigDecimal amount, String referenceId, String idempotencyKey);
    Wallet confirmDelivery(UUID sellerId, BigDecimal amount, String referenceId);
    Wallet confirmDelivery(UUID sellerId, BigDecimal amount, String referenceId, String idempotencyKey);
    void completeOrderPayment(UUID orderId, UUID listingId, UUID buyerId, UUID sellerId, BigDecimal amount);
    void refundOrderPayment(UUID orderId, UUID listingId, UUID buyerId, BigDecimal amount);
    List<Transaction> getTransactionHistory(UUID userId);
    Transaction getTransactionById(UUID transactionId, UUID userId);

    // User Deactivation 
    void releaseAllHoldsForUser(UUID userId);
}
