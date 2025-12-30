package com.cuttypaws.repository;

import com.cuttypaws.entity.Notification;
import com.cuttypaws.enums.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepo extends JpaRepository<Notification, Long> {

    Page<Notification> findByRecipientIdOrderByCreatedAtDesc(Long recipientId, Pageable pageable);

    long countByRecipientIdAndReadFalse(Long recipientId);

    List<Notification> findByRecipientIdAndReadFalse(Long recipientId);


    // Used to prevent duplicates for things like "like" or "new post"
    boolean existsByRecipientIdAndSenderIdAndTypeAndPostIdAndCommentId(
            Long recipientId,
            Long senderId,
            NotificationType type,
            Long postId,
            Long commentId
    );
}
