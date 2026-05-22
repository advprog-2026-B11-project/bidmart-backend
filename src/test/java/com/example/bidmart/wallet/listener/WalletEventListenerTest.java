package com.example.bidmart.wallet.listener;

import com.example.bidmart.common.event.UserDeactivatedEvent;
import com.example.bidmart.common.event.UserRegisteredEvent;
import com.example.bidmart.common.event.OrderDeliveredEvent;
import com.example.bidmart.common.event.OrderRefundedEvent;
import com.example.bidmart.wallet.service.WalletService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletEventListenerTest {

    @Mock private WalletService walletService;
    @InjectMocks private WalletEventListener listener;

    @Test
    void handleUserRegistered_createsWallet() {
        UUID userId = UUID.randomUUID();
        UserRegisteredEvent event = new UserRegisteredEvent(userId, "newuser", Instant.now());

        listener.handleUserRegistered(event);

        verify(walletService).createWallet(userId);
    }

    @Test
    void handleUserRegistered_errorDoesNotPropagate() {
        UUID userId = UUID.randomUUID();
        UserRegisteredEvent event = new UserRegisteredEvent(userId, "newuser", Instant.now());
        doThrow(new RuntimeException("DB error")).when(walletService).createWallet(userId);

        // Should not throw — error is caught and logged
        listener.handleUserRegistered(event);

        verify(walletService).createWallet(userId);
    }

    @Test
    void handleUserDeactivated_callsReleaseAllHolds() {
        UUID userId = UUID.randomUUID();
        UserDeactivatedEvent event = new UserDeactivatedEvent(userId, "testuser", Instant.now());

        listener.handleUserDeactivated(event);

        verify(walletService).releaseAllHoldsForUser(userId);
    }

    @Test
    void handleUserDeactivated_errorDoesNotPropagate() {
        UUID userId = UUID.randomUUID();
        UserDeactivatedEvent event = new UserDeactivatedEvent(userId, "testuser", Instant.now());
        doThrow(new RuntimeException("DB error")).when(walletService).releaseAllHoldsForUser(userId);

        listener.handleUserDeactivated(event);

        verify(walletService).releaseAllHoldsForUser(userId);
    }

    @Test
    void handleOrderDelivered_completesOrderPayment() {
        UUID orderId = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();
        UUID buyerId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();
        BigDecimal amount = BigDecimal.valueOf(100000);
        OrderDeliveredEvent event = new OrderDeliveredEvent(orderId, listingId, buyerId, sellerId, amount);

        listener.handleOrderDelivered(event);

        verify(walletService).completeOrderPayment(orderId, listingId, buyerId, sellerId, amount);
    }

    @Test
    void handleOrderRefunded_refundsOrderPayment() {
        UUID orderId = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();
        UUID buyerId = UUID.randomUUID();
        BigDecimal amount = BigDecimal.valueOf(100000);
        OrderRefundedEvent event = new OrderRefundedEvent(orderId, listingId, buyerId, amount);

        listener.handleOrderRefunded(event);

        verify(walletService).refundOrderPayment(orderId, listingId, buyerId, amount);
    }
}
