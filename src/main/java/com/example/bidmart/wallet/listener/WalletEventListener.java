package com.example.bidmart.wallet.listener;

import com.example.bidmart.common.event.UserDeactivatedEvent;
import com.example.bidmart.common.event.UserRegisteredEvent;
import com.example.bidmart.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class WalletEventListener {

    private final WalletService walletService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleUserRegistered(UserRegisteredEvent event) {
        try {
            log.info("Auto-creating wallet for new user: userId={}, username={}",
                    event.userId(), event.username());
            walletService.createWallet(event.userId());
            log.info("Wallet created for user: userId={}", event.userId());
        } catch (Exception e) {
            log.error("Error creating wallet for new user: userId={}", event.userId(), e);
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleUserDeactivated(UserDeactivatedEvent event) {
        try {
            log.info("Handling user deactivation for wallet: userId={}", event.userId());
            walletService.releaseAllHoldsForUser(event.userId());
            log.info("All holds released for deactivated user: userId={}", event.userId());
        } catch (Exception e) {
            log.error("Error releasing holds for deactivated user: userId={}", event.userId(), e);
        }
    }
}
