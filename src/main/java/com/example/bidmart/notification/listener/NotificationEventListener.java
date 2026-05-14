package com.example.bidmart.notification.listener;

import com.example.bidmart.common.event.AuctionWonEvent;
import com.example.bidmart.common.event.BidPlacedEvent;
import com.example.bidmart.common.event.OrderDeliveredEvent;
import com.example.bidmart.common.event.OutbidEvent;
import com.example.bidmart.common.event.OrderRefundedEvent;
import com.example.bidmart.common.event.AuctionClosedNoWinnerEvent;
import com.example.bidmart.common.event.AuctionExtendedEvent;
import com.example.bidmart.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationService notificationService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleAuctionWon(AuctionWonEvent event) {
        String message = "Selamat! Anda memenangkan lelang dengan harga akhir Rp " + event.winningPrice();
        notificationService.createNotification(event.winnerId(), "AUCTION_WON", message);

        String sellerMessage = "Barang lelang Anda (ID: " + event.listingId() + ") telah terjual dengan harga: " + event.winningPrice();
        notificationService.createNotification(event.sellerId(), "AUCTION_SOLD", sellerMessage);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleNewBid(BidPlacedEvent event) {
        String message = "Penawaran berhasil ditempatkan sebesar Rp " + event.bidAmount();
        notificationService.createNotification(event.buyerId(), "NEW_BID", message);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOutbid(OutbidEvent event) {
        String message = "Anda dikalahkan! Penawaran tertinggi sekarang: Rp " + event.newHighestBid();
        notificationService.createNotification(event.outbidUserId(), "OUTBID", message);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderDelivered(OrderDeliveredEvent event) {
        String sellerMessage = "Pesanan (ID: " + event.getOrderId() + ") telah dikonfirmasi diterima oleh pembeli. Dana sebesar Rp " + event.getAmount() + " sedang diteruskan ke dompet Anda.";
        notificationService.createNotification(event.getSellerId(), "ORDER_DELIVERED", sellerMessage);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderRefunded(OrderRefundedEvent event) {
        String message = "Sengketa pesanan (ID: " + event.getOrderId() + ") disetujui. Dana sebesar Rp " + event.getAmount() + " telah dikembalikan ke dompet Anda.";
        notificationService.createNotification(event.getBuyerId(), "ORDER_REFUNDED", message);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleAuctionClosedNoWinner(AuctionClosedNoWinnerEvent event) {
        String message = "Masa lelang untuk barang Anda (ID: " + event.listingId() + ") telah berakhir tanpa ada penawaran. Barang tidak terjual.";
        notificationService.createNotification(event.sellerId(), "AUCTION_CLOSED_NO_WINNER", message);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleAuctionExtended(AuctionExtendedEvent event) {
        String message = "Lelang barang Anda (ID: " + event.listingId() + ") otomatis diperpanjang karena ada penawaran di menit terakhir!";
        notificationService.createNotification(event.sellerId(), "AUCTION_EXTENDED", message);
    }
}