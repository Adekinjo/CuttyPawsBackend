package com.cuttypaws.service.impl;

import com.cuttypaws.dto.NotificationDto;
import com.cuttypaws.entity.PushToken;
import com.cuttypaws.repository.PushTokenRepo;
import com.cuttypaws.service.interf.MobilePushService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MobilePushServiceImpl implements MobilePushService {

    private final PushTokenRepo pushTokenRepo;
    private final WebClient webClient;

    @Value("${expo.push.url:https://exp.host/--/api/v2/push/send}")
    private String expoPushUrl;

    @Override
    public void registerToken(UUID userId, String token) {
        String normalizedToken = token.trim();

        PushToken pushToken = pushTokenRepo.findByToken(normalizedToken)
                .map(existing -> {
                    existing.setUserId(userId);
                    existing.setActive(true);
                    existing.setUpdatedAt(LocalDateTime.now());
                    return existing;
                })
                .orElse(
                        PushToken.builder()
                                .userId(userId)
                                .token(normalizedToken)
                                .active(true)
                                .createdAt(LocalDateTime.now())
                                .updatedAt(LocalDateTime.now())
                                .build()
                );

        pushTokenRepo.save(pushToken);
        log.info("Push token registered for user: {}", userId);
    }

    @Override
    public void removeToken(UUID userId, String token) {
        pushTokenRepo.findByToken(token.trim()).ifPresent(existing -> {
            if (existing.getUserId().equals(userId)) {
                existing.setActive(false);
                existing.setUpdatedAt(LocalDateTime.now());
                pushTokenRepo.save(existing);
                log.info("Push token deactivated for user: {}", userId);
            }
        });
    }

    @Override
    public void sendNotificationToUser(UUID userId, NotificationDto notification) {
        List<PushToken> tokens = pushTokenRepo.findByUserIdAndActiveTrue(userId);
        if (tokens.isEmpty()) {
            return;
        }

        for (PushToken pushToken : tokens) {
            try {
                Map<String, Object> data = new HashMap<>();
                data.put("notificationId", notification.getId());
                data.put("type", notification.getType() != null ? notification.getType().name() : null);
                data.put("postId", notification.getPostId());
                data.put("commentId", notification.getCommentId());
                data.put("senderId", notification.getSenderId());

                Map<String, Object> payload = new HashMap<>();
                payload.put("to", pushToken.getToken());
                payload.put("title", buildTitle(notification));
                payload.put("body", buildBody(notification));
                payload.put("sound", "default");
                payload.put("data", data);

                String response = webClient.post()
                        .uri(expoPushUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(payload)
                        .retrieve()
                        .bodyToMono(String.class)
                        .block();

                log.debug("Expo push response for user {}: {}", userId, response);

            } catch (Exception ex) {
                log.warn("Failed sending push notification to user {} token {}: {}",
                        userId, pushToken.getToken(), ex.getMessage());
            }
        }
    }

    private String buildTitle(NotificationDto notification) {
        if (notification.getType() == null) {
            return "CuttyPaws";
        }

        return switch (notification.getType()) {
            case NEW_POST -> "New post";
            case POST_LIKE -> "Post liked";
            case COMMENT -> "New comment";
            case REPLY -> "New reply";
            case COMMENT_LIKE -> "Comment liked";
            case COMMENT_REACTION -> "Comment reaction";
            case FOLLOW -> "New follower";
        };
    }

    private String buildBody(NotificationDto notification) {
        String senderName = notification.getSenderName() != null ? notification.getSenderName() : "Someone";
        String message = notification.getMessage() != null ? notification.getMessage() : "sent you an update";

        if (message.toLowerCase().startsWith(senderName.toLowerCase())) {
            return message;
        }

        return senderName + " " + message;
    }
}
