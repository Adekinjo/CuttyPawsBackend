package com.cuttypaws.controller;

import com.cuttypaws.dto.NotificationDto;
import com.cuttypaws.response.NotificationResponse;
import com.cuttypaws.security.CurrentUser;
import com.cuttypaws.service.interf.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<NotificationResponse> getMyNotifications(
            @CurrentUser Long userId,
            @RequestParam(defaultValue="0") int page,
            @RequestParam(defaultValue="20") int size
    ) {
        Page<NotificationDto> result = notificationService.getMyNotifications(userId, page, size);

        return ResponseEntity.ok(
                NotificationResponse.builder()
                        .status(200)
                        .message("Notifications fetched")
                        .notificationList(result.getContent())
                        .totalPages(result.getTotalPages())
                        .totalElement(result.getTotalElements())
                        .build()
        );
    }

    @GetMapping("/unread-count")
    public ResponseEntity<NotificationResponse> unreadCount(@CurrentUser Long userId) {
        long count = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(NotificationResponse.builder()
                .status(200)
                .message("Unread count fetched")
                .totalComments((int) count) // or add unreadCount field if you want
                .build());
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<NotificationResponse> markRead(
            @CurrentUser Long userId,
            @PathVariable Long id
    ) {
        notificationService.markAsRead(userId, id);
        return ResponseEntity.ok(NotificationResponse.builder()
                .status(200)
                .message("Marked as read")
                .build());
    }

    @PutMapping("/read-all")
    public ResponseEntity<NotificationResponse> markAllRead(@CurrentUser Long userId) {
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(NotificationResponse.builder()
                .status(200)
                .message("All notifications marked as read")
                .build());
    }
}
