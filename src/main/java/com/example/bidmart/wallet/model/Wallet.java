package com.example.bidmart.wallet.model;

import jakarta.persistence.*;
import java.util.UUID;
import java.math.BigDecimal;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

@Entity
@Table(name = "wallets")
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(nullable = false)
    private BigDecimal balanceAvailable = BigDecimal.ZERO;

    @Column(nullable = false)
    private BigDecimal balanceLocked = BigDecimal.ZERO;

    public Wallet(UUID userId) {
        this.userId = userId;
        this.balanceAvailable = BigDecimal.ZERO;
        this.balanceLocked = BigDecimal.ZERO;
    }
}