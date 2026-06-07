package com.example.bidmart.notification.delivery;

import com.example.bidmart.notification.model.Notification;
import com.example.bidmart.notification.model.NotificationPreference;
import com.example.bidmart.user.model.User;
import com.example.bidmart.user.repository.UserRepository;
import com.example.bidmart.user.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailNotificationDeliveryChannel implements NotificationDeliveryChannel {

    private final EmailService emailService;
    private final UserRepository userRepository;

    @Override
    public boolean supports(NotificationPreference preference) {
        return preference.isEmailEnabled();
    }

    @Override
    public void send(Notification notification, NotificationPreference preference) {
        userRepository.findById(notification.getUserId()).ifPresentOrElse(
            user -> {
                try {
                    String subject = "Notifikasi BidMart - " + notification.getType();
                    String body = "<div style=\"font-family:Arial, sans-serif; font-size:14px;\">"
                            + "<p>Halo " + user.getDisplayName() + ",</p>"
                            + "<p>Ada notifikasi baru untuk Anda:</p>"
                            + "<p style=\"font-weight:bold;font-size:16px;\">" + notification.getMessage() + "</p>"
                            + "<p>— Tim BidMart</p>"
                            + "</div>";
                    emailService.sendNotificationEmail(user.getEmail(), subject, body);
                } catch (Exception e) {
                    log.error("Gagal mengirim notifikasi email ke {}: {}", user.getEmail(), e.getMessage());
                    throw new RuntimeException("Gagal mengirim email", e);
                }
            },
            () -> {
                log.warn("User tidak ditemukan untuk ID: {}", notification.getUserId());
            }
        );
    }
}
