package com.example.bidmart.wallet.controller;

import com.example.bidmart.user.service.UserService;
import com.example.bidmart.wallet.dto.TopUpRequest;
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

    @PostMapping("/me/top-up")
    public ResponseEntity<Wallet> topUpMyWallet(
            Authentication authentication,
            @RequestBody TopUpRequest request
    ) {
        UUID userId = resolveCurrentUserId(authentication);
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

    private UUID resolveCurrentUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User belum terautentikasi.");
        }

        return userService.getUserIdByUsername(authentication.getName());
    }

    private void ensureCurrentUser(UUID userId, Authentication authentication) {
        UUID authenticatedUserId = resolveCurrentUserId(authentication);
        if (!authenticatedUserId.equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Anda tidak dapat mengakses wallet milik user lain.");
        }
    }
}
