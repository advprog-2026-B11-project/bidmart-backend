package com.example.bidmart.wallet.controller;

import com.example.bidmart.wallet.dto.*;
import com.example.bidmart.wallet.model.Transaction;
import com.example.bidmart.wallet.model.Wallet;
import com.example.bidmart.wallet.service.WalletService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/wallet")
@CrossOrigin(origins = "*")
public class WalletController {

    private final WalletService walletService;
    private final UserService userService;

    public WalletController(WalletService walletService, UserService userService) {
        this.walletService = walletService;
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<Wallet> createWallet(Authentication authentication) {
        UUID userId = resolveCurrentUserId(authentication);
        Wallet wallet = walletService.createWallet(userId);

        if (wallet == null) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(wallet);
    }

    @GetMapping("/me")
    public ResponseEntity<Wallet> getMyBalance(Authentication authentication) {
        UUID userId = resolveCurrentUserId(authentication);
        Wallet wallet = walletService.getWalletByUserId(userId);

        if (wallet == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(wallet);
    }

    @PostMapping("/{userId}/top-up")
    public ResponseEntity<Wallet> topUp(@PathVariable UUID userId, @RequestBody TopUpRequest request) {
        Wallet wallet = walletService.topUp(userId, request.getAmount());

        if (wallet == null) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(wallet);
    }

    @GetMapping("/{userId}/balance")
    public ResponseEntity<Wallet> getBalance(
            @PathVariable UUID userId,
            Authentication authentication
    ) {
        ensureCurrentUser(userId, authentication);
        return getMyBalance(authentication);
    }

    @PostMapping("/{userId}/top-up")
    public ResponseEntity<Wallet> topUp(
            @PathVariable UUID userId,
            @RequestBody TopUpRequest request,
            Authentication authentication
    ) {
        ensureCurrentUser(userId, authentication);
        return topUpMyWallet(authentication, request);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/list")
    public List<Wallet> getAllWallets() {
        return walletService.findAll();
    }

    @PostMapping("/{userId}/hold")
    public ResponseEntity<Wallet> holdBalance(@PathVariable UUID userId, @RequestBody HoldBalanceRequest request) {
        Wallet wallet = walletService.reserveBidFunds(userId, request.getListingId(), request.getAmount());
        if (wallet == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(wallet);
    }

    @PostMapping("/{userId}/release")
    public ResponseEntity<Wallet> releaseHold(@PathVariable UUID userId, @RequestBody HoldBalanceRequest request) {
        Wallet wallet = walletService.releaseBidFunds(userId, request.getListingId(), request.getAmount());

        if (wallet == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(wallet);
    }

    @PostMapping("/{userId}/settle")
    public ResponseEntity<Wallet> settlePayment(@PathVariable UUID userId, @RequestBody HoldBalanceRequest request) {
        Wallet wallet = walletService.settlePayment(userId, request.getAmount(), request.getListingId().toString());
        
        if (wallet == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(wallet);
    }

    @PostMapping("/{userId}/withdraw")
    public ResponseEntity<Wallet> withdraw(@PathVariable UUID userId, @RequestBody WithdrawRequest request) {
        Wallet wallet = walletService.withdraw(userId, request.getAmount());
        
        if (wallet == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(wallet);
    }

    @GetMapping("/{userId}/transactions")
    public ResponseEntity<List<Transaction>> getTransactionHistory(@PathVariable UUID userId) {
        List<Transaction> transactions = walletService.getTransactionHistory(userId);
        
        if (transactions == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(transactions);
    }

    @PostMapping("/confirm-delivery")
    public ResponseEntity<Wallet> confirmDelivery(@RequestBody ConfirmDeliveryRequest request) {
        Wallet wallet = walletService.confirmDelivery(
            request.getSellerId(), request.getAmount(), request.getListingId().toString());

        if (wallet == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(wallet);
    }
}
