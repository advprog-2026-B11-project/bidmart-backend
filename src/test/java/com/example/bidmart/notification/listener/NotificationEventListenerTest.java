package com.example.bidmart.notification.listener;

import com.example.bidmart.common.event.*;
import com.example.bidmart.notification.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationEventListenerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationEventListener notificationEventListener;

    @Test
    void handleAuctionWon_success() {
        UUID winnerId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();
        BigDecimal price = BigDecimal.valueOf(50000);

        AuctionWonEvent event = new AuctionWonEvent(listingId, winnerId, sellerId, price);
        notificationEventListener.handleAuctionWon(event);

        verify(notificationService, times(1)).createNotification(eq(winnerId), eq("AUCTION_WON"), anyString());
        verify(notificationService, times(1)).createNotification(eq(sellerId), eq("AUCTION_SOLD"), anyString());
    }

    @Test
    void handleNewBid_success() {
        UUID buyerId = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();
        BigDecimal amount = BigDecimal.valueOf(10000);

        BidPlacedEvent event = new BidPlacedEvent(UUID.randomUUID(), listingId, buyerId, amount);
        notificationEventListener.handleNewBid(event);

        verify(notificationService, times(1)).createNotification(eq(buyerId), eq("NEW_BID"), anyString());
    }

    @Test
    void handleOutbid_success() {
        UUID outbidUserId = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();
        BigDecimal highestBid = BigDecimal.valueOf(20000);

        OutbidEvent event = new OutbidEvent(listingId, outbidUserId, highestBid);
        notificationEventListener.handleOutbid(event);

        verify(notificationService, times(1)).createNotification(eq(outbidUserId), eq("OUTBID"), anyString());
    }

    @Test
    void handleOrderDelivered_success() {
        UUID orderId = UUID.randomUUID();
        UUID buyerId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();
        BigDecimal amount = BigDecimal.valueOf(50000);

        OrderDeliveredEvent event = new OrderDeliveredEvent(orderId, buyerId, sellerId, amount);
        notificationEventListener.handleOrderDelivered(event);

        verify(notificationService, times(1)).createNotification(eq(sellerId), eq("ORDER_DELIVERED"), anyString());
    }

    @Test
    void handleOrderRefunded_success() {
        UUID orderId = UUID.randomUUID();
        UUID buyerId = UUID.randomUUID();
        BigDecimal amount = BigDecimal.valueOf(50000);

        OrderRefundedEvent event = new OrderRefundedEvent(orderId, buyerId, amount);
        notificationEventListener.handleOrderRefunded(event);

        verify(notificationService, times(1)).createNotification(eq(buyerId), eq("ORDER_REFUNDED"), anyString());
    }

    @Test
    void handleAuctionClosedNoWinner_success() {
        UUID listingId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();

        AuctionClosedNoWinnerEvent event = new AuctionClosedNoWinnerEvent(listingId, sellerId);
        notificationEventListener.handleAuctionClosedNoWinner(event);

        verify(notificationService, times(1)).createNotification(eq(sellerId), eq("AUCTION_CLOSED_NO_WINNER"), anyString());
    }

    @Test
    void handleAuctionExtended_success() {
        UUID listingId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();

        AuctionExtendedEvent event = new AuctionExtendedEvent(listingId, sellerId);
        notificationEventListener.handleAuctionExtended(event);

        verify(notificationService, times(1)).createNotification(eq(sellerId), eq("AUCTION_EXTENDED"), anyString());
    }

    @Test
    void handleBalanceTopUp_success() {
        UUID userId = UUID.randomUUID();
        BigDecimal amount = BigDecimal.valueOf(50000);

        BalanceTopUpEvent event = new BalanceTopUpEvent(userId, amount, "BANK");
        notificationEventListener.handleBalanceTopUp(event);

        verify(notificationService, times(1)).createNotification(eq(userId), eq("BALANCE_TOPUP"), anyString());
    }

    @Test
    void handleBalanceHeld_success() {
        UUID userId = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();
        BigDecimal amount = BigDecimal.valueOf(30000);

        BalanceHeldEvent event = new BalanceHeldEvent(userId, amount, listingId);
        notificationEventListener.handleBalanceHeld(event);

        verify(notificationService, times(1)).createNotification(eq(userId), eq("BALANCE_HELD"), anyString());
    }

    @Test
    void handleBalanceReleased_success() {
        UUID userId = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();
        BigDecimal amount = BigDecimal.valueOf(30000);

        BalanceReleasedEvent event = new BalanceReleasedEvent(userId, amount, listingId);
        notificationEventListener.handleBalanceReleased(event);

        verify(notificationService, times(1)).createNotification(eq(userId), eq("BALANCE_RELEASED"), anyString());
    }

    @Test
    void handleWithdraw_success() {
        UUID userId = UUID.randomUUID();
        BigDecimal amount = BigDecimal.valueOf(25000);

        WithdrawEvent event = new WithdrawEvent(userId, amount, "BCA");
        notificationEventListener.handleWithdraw(event);

        verify(notificationService, times(1)).createNotification(eq(userId), eq("BALANCE_WITHDRAW"), anyString());
    }

    @Test
    void handleBalanceSettled_success() {
        UUID userId = UUID.randomUUID();
        BigDecimal amount = BigDecimal.valueOf(100000);

        BalanceSettledEvent event = new BalanceSettledEvent(userId, amount, "REF-001");
        notificationEventListener.handleBalanceSettled(event);

        verify(notificationService, times(1)).createNotification(eq(userId), eq("BALANCE_SETTLED"), anyString());
    }

    @Test
    void handleBalanceIncome_success() {
        UUID userId = UUID.randomUUID();
        BigDecimal amount = BigDecimal.valueOf(80000);

        BalanceIncomeEvent event = new BalanceIncomeEvent(userId, amount, "REF-002");
        notificationEventListener.handleBalanceIncome(event);

        verify(notificationService, times(1)).createNotification(eq(userId), eq("BALANCE_INCOME"), anyString());
    }
}