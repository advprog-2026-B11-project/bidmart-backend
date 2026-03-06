package com.example.bidmart.wallet.controller;

import com.example.bidmart.wallet.dto.CreateWalletRequest;
import com.example.bidmart.wallet.dto.TopUpRequest;
import com.example.bidmart.wallet.model.Wallet;
import com.example.bidmart.wallet.service.WalletService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/wallet")
@CrossOrigin(origins = "*")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @PostMapping("/register")
    public ResponseEntity<Wallet> createWallet(@RequestBody CreateWalletRequest request) {
        Wallet wallet = walletService.createWallet(request.getUserId());

        if (wallet == null) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(wallet);
    }

    @GetMapping("/{userId}/balance")
    public ResponseEntity<Wallet> getBalance(@PathVariable UUID userId) {
        Wallet wallet = walletService.getWalletByUserId(userId);

        if (wallet == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(wallet);
    }

    @PostMapping("/{userId}/top-up")
    public ResponseEntity<Wallet> topUp(@PathVariable UUID userId,
                                        @RequestBody TopUpRequest request) {
        Wallet wallet = walletService.topUp(userId, request.getAmount());

        if (wallet == null) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(wallet);
    }
}