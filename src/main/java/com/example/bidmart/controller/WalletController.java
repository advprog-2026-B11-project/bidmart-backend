package com.example.bidmart.controller;

import com.example.bidmart.model.Wallet;
import com.example.bidmart.repository.WalletRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/wallet")
@CrossOrigin(origins = "*")
public class WalletController {

    @Autowired
    private WalletRepository walletRepository;

    @GetMapping
    public ResponseEntity<List<Wallet>> getAllWallets() {
        return ResponseEntity.ok(walletRepository.findAll());
    }

    @GetMapping("/init-dummy")
    public ResponseEntity<Wallet> createDummyWallet(@RequestParam String username) {
        Wallet newWallet = new Wallet(username, new BigDecimal("10000.00"));
        Wallet savedWallet = walletRepository.save(newWallet);
        return ResponseEntity.ok(savedWallet);
    }
}