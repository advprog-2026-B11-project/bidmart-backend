package com.example.bidmart.notification.service;

import com.example.bidmart.notification.model.Notification;
import com.example.bidmart.notification.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.StopWatch;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=false",
    "spring.datasource.driverClassName=org.h2.Driver",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.datasource.username=sa",
    "spring.datasource.password=password",
    "jwt.secret=testingsecretkeywhichislongenoughforhmacsha256algorithm"
})
public class NotificationProfilingTest {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private NotificationRepository notificationRepository;

    @Test
    public void testProfilingMarkAllAsRead() {
        UUID userId = UUID.randomUUID();
        
        System.out.println("Seeding 5000 dummy notifications for user: " + userId);
        List<Notification> notifications = new ArrayList<>();
        for (int i = 0; i < 5000; i++) {
            notifications.add(Notification.builder()
                    .userId(userId)
                    .type("INFO")
                    .message("Test Notification " + i)
                    .isRead(false)
                    .createdAt(LocalDateTime.now())
                    .deliveryStatus("DELIVERED")
                    .build());
        }
        notificationRepository.saveAll(notifications);

        System.out.println("\n[START] Profiling markAllAsRead() ...");
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        
        notificationService.markAllAsRead(userId);
        
        stopWatch.stop();

        System.out.println("\n========================================================");
        System.out.println("             PROFILING RESULT (AFTER)                  ");
        System.out.println("========================================================");
        System.out.println("Method         : NotificationService.markAllAsRead()");
        System.out.println("Records Processed : 5000");
        System.out.println("Execution Time : " + stopWatch.getTotalTimeMillis() + " ms");
        System.out.println("========================================================\n");
    }
}
