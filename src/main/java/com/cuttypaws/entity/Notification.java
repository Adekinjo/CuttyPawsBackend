package com.cuttypaws.entity;

import com.cuttypaws.enums.NotificationType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications",
        indexes = {
                @Index(name="idx_notifications_recipient", columnList = "recipient_id"),
                @Index(name="idx_notifications_read", columnList = "is_read")
        }
)
@Getter @Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // person receiving notification
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="recipient_id", nullable = false)
    private User recipient;

    // person that caused the event (can be null for system)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="sender_id")
    private User sender;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationType type;

    @Column(nullable = false, length = 600)
    private String message;

    // optional references
    private Long postId;
    private Long commentId;

    @Builder.Default
    @Column(name="is_read", nullable = false)
    private boolean read = false;

    @Builder.Default
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
