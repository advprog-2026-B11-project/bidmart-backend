package com.example.bidmart.wallet.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.math.BigDecimal;

@Entity
public class Wallet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;
    private BigDecimal balance;

    public Wallet() {}

    public Wallet(String username, BigDecimal balance) {
        this.username = username;
        this.balance = balance;
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public BigDecimal getBalance() { return balance; }
}