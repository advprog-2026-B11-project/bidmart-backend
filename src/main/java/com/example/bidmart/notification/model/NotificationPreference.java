package com.example.bidmart.notification.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.Set;
import java.util.HashSet;
import java.util.UUID;

@Entity
@Table(name = "notification_preferences")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(name = "email_enabled", nullable = false)
    private boolean emailEnabled;

    @Column(name = "push_enabled", nullable = false)
    private boolean pushEnabled;

    @Column(name = "in_app_enabled", nullable = false)
    private boolean inAppEnabled;

    @Builder.Default
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "notification_muted_types", joinColumns = @JoinColumn(name = "preference_id"))
    @Column(name = "muted_type")
    private Set<String> mutedTypes = new HashSet<>();

    public NotificationPreference(UUID id, UUID userId, boolean emailEnabled, boolean pushEnabled, boolean inAppEnabled) {
        this.id = id;
        this.userId = userId;
        this.emailEnabled = emailEnabled;
        this.pushEnabled = pushEnabled;
        this.inAppEnabled = inAppEnabled;
        this.mutedTypes = new HashSet<>();
    }
}