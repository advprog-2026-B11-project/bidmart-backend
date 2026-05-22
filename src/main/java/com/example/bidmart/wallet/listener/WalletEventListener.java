package com.example.bidmart.wallet.listener;

import com.example.bidmart.common.event.UserDeactivatedEvent;
import com.example.bidmart.common.event.UserRegisteredEvent;
import com.example.bidmart.common.event.OrderDeliveredEvent;
import com.example.bidmart.common.event.OrderRefundedEvent;
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

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderDelivered(OrderDeliveredEvent event) {
        try {
            log.info("Completing order payment: orderId={}, buyerId={}, sellerId={}, amount={}",
                    event.getOrderId(), event.getBuyerId(), event.getSellerId(), event.getAmount());
            walletService.completeOrderPayment(
                    event.getOrderId(),
                    event.getListingId(),
                    event.getBuyerId(),
                    event.getSellerId(),
                    event.getAmount()
            );
            log.info("Order payment completed: orderId={}", event.getOrderId());
        } catch (Exception e) {
            log.error("Error completing order payment: orderId={}", event.getOrderId(), e);
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderRefunded(OrderRefundedEvent event) {
        try {
            log.info("Refunding order payment: orderId={}, buyerId={}, amount={}",
                    event.getOrderId(), event.getBuyerId(), event.getAmount());
            walletService.refundOrderPayment(
                    event.getOrderId(),
                    event.getListingId(),
                    event.getBuyerId(),
                    event.getAmount()
            );
            log.info("Order payment refunded: orderId={}", event.getOrderId());
        } catch (Exception e) {
            log.error("Error refunding order payment: orderId={}", event.getOrderId(), e);
        }
    }
}
