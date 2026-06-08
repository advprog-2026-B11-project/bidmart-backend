package com.example.bidmart.notification.listener;

import com.example.bidmart.common.event.AuctionWonEvent;
import com.example.bidmart.common.event.BalanceHeldEvent;
import com.example.bidmart.common.event.BalanceIncomeEvent;
import com.example.bidmart.common.event.BalanceReleasedEvent;
import com.example.bidmart.common.event.BalanceSettledEvent;
import com.example.bidmart.common.event.BalanceTopUpEvent;
import com.example.bidmart.common.event.BidPlacedEvent;
import com.example.bidmart.common.event.OrderDeliveredEvent;
import com.example.bidmart.common.event.OutbidEvent;
import com.example.bidmart.common.event.UserDeactivatedEvent;
import com.example.bidmart.common.event.UserRoleChangedEvent;
import com.example.bidmart.common.event.OrderRefundedEvent;
import com.example.bidmart.common.event.AuctionClosedNoWinnerEvent;
import com.example.bidmart.common.event.AuctionExtendedEvent;
import com.example.bidmart.common.event.WithdrawEvent;
import com.example.bidmart.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
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

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleBalanceTopUp(BalanceTopUpEvent event) {
        String message = String.format(
                "Top-up berhasil! Anda telah menambahkan Rp %s ke dompet Anda melalui %s.",
                String.format("%,.0f", event.amount()), event.paymentMethod()
        );
        notificationService.createNotification(event.userId(), "BALANCE_TOPUP", message);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleBalanceHeld(BalanceHeldEvent event) {
        String message = String.format(
                "Saldo Rp %s telah dikunci untuk penawaran pada listing (ID: %s).",
                String.format("%,.0f", event.amount()), event.listingId()
        );
        notificationService.createNotification(event.userId(), "BALANCE_HELD", message);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleBalanceReleased(BalanceReleasedEvent event) {
        String message = String.format(
                "Saldo Rp %s telah dilepas kembali ke dompet Anda dari listing (ID: %s).",
                String.format("%,.0f", event.amount()), event.listingId()
        );
        notificationService.createNotification(event.userId(), "BALANCE_RELEASED", message);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleWithdraw(WithdrawEvent event) {
        String rawDest = event.bankName();
        String destFormatted = formatWithdrawalDestination(rawDest);
        String message;
        if (rawDest != null && rawDest.contains("phoneNumber")) {
            message = String.format(
                    "Penarikan berhasil! Rp %s sedang diproses ke akun %s Anda.",
                    String.format("%,.0f", event.amount()), destFormatted
            );
        } else {
            message = String.format(
                    "Penarikan berhasil! Rp %s sedang diproses ke rekening bank %s Anda.",
                    String.format("%,.0f", event.amount()), destFormatted
            );
        }
        notificationService.createNotification(event.userId(), "BALANCE_WITHDRAW", message);
    }

    private String formatWithdrawalDestination(String destination) {
        if (destination == null) {
            return "tujuan Anda";
        }
        
        String cleaned = destination.trim();
        if (cleaned.startsWith("{") && cleaned.endsWith("}")) {
            cleaned = cleaned.substring(1, cleaned.length() - 1);
        }
        
        String bankName = null;
        String accountNumber = null;
        String phoneNumber = null;
        
        String[] pairs = cleaned.split(",\\s*");
        for (String pair : pairs) {
            String[] kv = pair.split("=");
            if (kv.length == 2) {
                String key = kv[0].trim();
                String val = kv[1].trim();
                if ("bankName".equalsIgnoreCase(key)) {
                    bankName = val;
                } else if ("accountNumber".equalsIgnoreCase(key)) {
                    accountNumber = val;
                } else if ("phoneNumber".equalsIgnoreCase(key)) {
                    phoneNumber = val;
                }
            }
        }
        
        if (phoneNumber != null) {
            String maskedPhone = maskPhone(phoneNumber);
            return "GoPay (" + maskedPhone + ")";
        } else if (bankName != null && accountNumber != null) {
            return bankName + " (Rekening: " + accountNumber + ")";
        } else if (bankName != null) {
            return bankName;
        }
        
        return destination;
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() <= 8) return "****";
        return phone.substring(0, 4) + "****" + phone.substring(phone.length() - 4);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleBalanceSettled(BalanceSettledEvent event) {
        String message = String.format(
                "Pembayaran sebesar Rp %s telah diselesaikan untuk transaksi (Ref: %s).",
                String.format("%,.0f", event.amount()), event.referenceId()
        );
        notificationService.createNotification(event.userId(), "BALANCE_SETTLED", message);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleBalanceIncome(BalanceIncomeEvent event) {
        String message = String.format(
                "Anda menerima pendapatan sebesar Rp %s dari penjualan (Ref: %s). Saldo telah ditambahkan ke dompet Anda.",
                String.format("%,.0f", event.amount()), event.referenceId()
        );
        notificationService.createNotification(event.userId(), "BALANCE_INCOME", message);
    }
}
