package com.cuttypaws.dto;

import com.cuttypaws.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NotificationDto {
    private Long id;

    private UUID recipientId;

    private UUID senderId;
    private String senderName;
    private String senderProfileImage;

    private NotificationType type;
    private String message;

    private Long postId;
    private Long commentId;

    private boolean read;
    private LocalDateTime createdAt;
}
