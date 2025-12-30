package com.cuttypaws.service.impl;

import com.cuttypaws.dto.*;
import com.cuttypaws.entity.Notification;
import com.cuttypaws.entity.*;
import com.cuttypaws.enums.NotificationType;
import com.cuttypaws.exception.NotFoundException;
import com.cuttypaws.repository.*;
import com.cuttypaws.service.interf.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepo notificationRepo;
    private final UserRepo userRepo;
    private final SimpMessagingTemplate messagingTemplate;

    // ==============================
    // COMMON SENDER (NO DUPLICATE)
    // ==============================
    private void sendNotification(
            Long recipientId,
            Long senderId,
            NotificationType type,
            String message,
            Long postId,
            Long commentId,
            boolean dedupe
    ) {
        if (recipientId == null) return;

        // do not notify yourself
        if (senderId != null && senderId.equals(recipientId)) return;

        if (dedupe && senderId != null) {
            boolean exists = notificationRepo.existsByRecipientIdAndSenderIdAndTypeAndPostIdAndCommentId(
                    recipientId, senderId, type, postId, commentId
            );
            if (exists) {
                log.debug("🔁 Duplicate notification skipped: recipient={}, sender={}, type={}",
                        recipientId, senderId, type);
                return;
            }
        }

        User recipient = userRepo.findById(recipientId)
                .orElseThrow(() -> new NotFoundException("Recipient not found"));

        User sender = null;
        if (senderId != null) {
            sender = userRepo.findById(senderId)
                    .orElseThrow(() -> new NotFoundException("Sender not found"));
        }

        Notification saved = notificationRepo.save(
                Notification.builder()
                        .recipient(recipient)
                        .sender(sender)
                        .type(type)
                        .message(message)
                        .postId(postId)
                        .commentId(commentId)
                        .read(false)
                        .build()
        );

        NotificationDto dto = mapToDto(saved);

        // ✅ WebSocket emit:
        // subscriber should listen to: /topic/notifications/{userId}
        messagingTemplate.convertAndSend("/topic/notifications/" + recipientId, dto);
    }

    private NotificationDto mapToDto(Notification n) {
        return NotificationDto.builder()
                .id(n.getId())
                .recipientId(n.getRecipient().getId())
                .senderId(n.getSender() != null ? n.getSender().getId() : null)
                .senderName(n.getSender() != null ? n.getSender().getName() : null)
                .senderProfileImage(n.getSender() != null ? n.getSender().getProfileImageUrl() : null)
                .type(n.getType())
                .message(n.getMessage())
                .postId(n.getPostId())
                .commentId(n.getCommentId())
                .read(n.isRead())
                .createdAt(n.getCreatedAt())
                .build();
    }

    // ==============================
    // READ / FETCH
    // ==============================
    @Override
    @Transactional(readOnly = true)
    public Page<NotificationDto> getMyNotifications(Long userId, int page, int size) {
        try {
            log.info("🔍 Fetching notifications for user: {}, page: {}, size: {}", userId, page, size);

            // FIX: Remove Sort.by since repository method already includes ordering
            Pageable pageable = PageRequest.of(page, size);
            log.info("📋 Pageable created: {}", pageable);

            Page<Notification> result = notificationRepo.findByRecipientIdOrderByCreatedAtDesc(userId, pageable);
            log.info("✅ Notifications fetched successfully. Count: {}", result.getContent().size());

            return result.map(this::mapToDto);
        } catch (Exception e) {
            log.error("❌ Error fetching notifications for user {}: {}", userId, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return notificationRepo.countByRecipientIdAndReadFalse(userId);
    }

    @Override
    @Transactional
    public void markAsRead(Long userId, Long notificationId) {
        Notification n = notificationRepo.findById(notificationId)
                .orElseThrow(() -> new NotFoundException("Notification not found"));

        if (!n.getRecipient().getId().equals(userId)) {
            throw new NotFoundException("Notification not found for this user");
        }
        n.setRead(true);
        notificationRepo.save(n);
    }

    @Override
    @Transactional
    public void markAllAsRead(Long userId) {
        // FIX: Use simpler query without ordering for updates
        List<Notification> unreadNotifications = notificationRepo.findByRecipientIdAndReadFalse(userId);

        log.info("📝 Marking {} notifications as read for user: {}", unreadNotifications.size(), userId);

        unreadNotifications.forEach(n -> n.setRead(true));
        notificationRepo.saveAll(unreadNotifications);

        log.info("✅ Successfully marked {} notifications as read", unreadNotifications.size());
    }

    // ==============================
    // TRIGGERS
    // ==============================
    @Override
    public void notifyFollowersNewPost(Post post, User owner, List<Long> followerIds) {
        if (followerIds == null || followerIds.isEmpty()) return;

        log.info("📢 Notifying {} followers about new post: {}", followerIds.size(), post.getId());

        for (Long followerId : followerIds) {
            sendNotification(
                    followerId,
                    owner.getId(),
                    NotificationType.NEW_POST,
                    owner.getName() + " posted a new update",
                    post.getId(),
                    null,
                    true // dedupe on (follower, owner, NEW_POST, post)
            );
        }
    }

    @Override
    public void notifyPostLiked(Post post, User liker) {
        log.info("❤️ Notifying post owner about like: post={}, liker={}", post.getId(), liker.getId());

        sendNotification(
                post.getOwner().getId(),
                liker.getId(),
                NotificationType.POST_LIKE,
                liker.getName() + " liked your post",
                post.getId(),
                null,
                true // dedupe likes
        );
    }

    @Override
    public void notifyCommented(Post post, Comment comment, User commenter) {
        log.info("💬 Notifying post owner about comment: post={}, commenter={}", post.getId(), commenter.getId());

        sendNotification(
                post.getOwner().getId(),
                commenter.getId(),
                NotificationType.COMMENT,
                commenter.getName() + " commented on your post",
                post.getId(),
                comment.getId(),
                false // allow multiple comments
        );
    }

    @Override
    public void notifyReply(Comment parent, Comment reply, User replier) {
        User parentOwner = parent.getUser();

        log.info("↩️ Notifying comment owner about reply: comment={}, replier={}", parent.getId(), replier.getId());

        sendNotification(
                parentOwner.getId(),
                replier.getId(),
                NotificationType.REPLY,
                replier.getName() + " replied to your comment",
                parent.getPost().getId(),
                reply.getId(),
                false
        );
    }
    @Override
    public void sendFollowNotification(Long recipientId, Long senderId) {
        sendNotification(
                recipientId,               // who receives the notification
                senderId,                  // follower
                NotificationType.FOLLOW,
                "started following you",
                null,
                null,
                true           // dedupe, user cannot follow twice
        );
    }

}