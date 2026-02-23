package com.cuttypaws.repository;

import com.cuttypaws.entity.Notification;
import com.cuttypaws.enums.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NotificationRepo extends JpaRepository<Notification, Long> {

    Page<Notification> findByRecipientIdOrderByCreatedAtDesc(UUID recipientId, Pageable pageable);

    long countByRecipientIdAndReadFalse(UUID recipientId);

    List<Notification> findByRecipientIdAndReadFalse(UUID recipientId);


    // Used to prevent duplicates for things like "like" or "new post"
    boolean existsByRecipientIdAndSenderIdAndTypeAndPostIdAndCommentId(
            UUID recipientId,
            UUID senderId,
            NotificationType type,
            Long postId,
            Long commentId
    );
}
