package com.example.bidmart.wallet.repository;

import com.example.bidmart.wallet.model.Transaction;
import com.example.bidmart.wallet.model.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    List<Transaction> findByWalletIdOrderByCreatedAtDesc(UUID walletId);
    Optional<Transaction> findByReferenceIdAndType(String referenceId, String type);
    List<Transaction> findByWalletIdAndReferenceId(UUID walletId, String referenceId);
    List<Transaction> findByWalletIdAndType(UUID walletId, TransactionType type);
    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);

    @Query("SELECT t FROM Transaction t JOIN Wallet w ON t.walletId = w.id WHERE w.userId = :userId ORDER BY t.createdAt DESC")
    List<Transaction> findByUserId(@Param("userId") UUID userId);

    @Query(value = "SELECT COALESCE(SUM(CASE WHEN type = 'HOLD' THEN amount " +
           "WHEN type IN ('REFUND', 'PAYMENT') THEN -amount " +
           "ELSE 0 END), 0) " +
           "FROM transactions WHERE wallet_id = :walletId AND reference_id = :referenceId", nativeQuery = true)
    BigDecimal calculateNetHeldAmount(@Param("walletId") UUID walletId, @Param("referenceId") String referenceId);
}
