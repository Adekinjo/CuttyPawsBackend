package com.cuttypaws.service.interf;

import com.cuttypaws.dto.NotificationDto;
import com.cuttypaws.entity.Comment;
import com.cuttypaws.entity.Post;
import com.cuttypaws.entity.User;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.UUID;

public interface NotificationService {

    // fetch + read
    Page<NotificationDto> getMyNotifications(UUID userId, int page, int size);
    long getUnreadCount(UUID userId);
    void markAsRead(UUID userId, Long notificationId);
    void markAllAsRead(UUID userId);

    // triggers (used by other services)
    void notifyFollowersNewPost(Post post, User owner, List<UUID> followerIds);
    void notifyPostLiked(Post post, User liker);
    void notifyCommented(Post post, Comment comment, User commenter);
    void notifyReply(Comment parent, Comment reply, User replier);
    void sendFollowNotification(UUID recipientId, UUID senderId);

}
