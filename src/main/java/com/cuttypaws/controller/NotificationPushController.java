package com.cuttypaws.controller;

import com.cuttypaws.dto.PushTokenRequest;
import com.cuttypaws.response.NotificationResponse;
import com.cuttypaws.security.CurrentUser;
import com.cuttypaws.service.interf.MobilePushService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationPushController {

    private final MobilePushService mobilePushService;

    @PostMapping("/push-token")
    public ResponseEntity<NotificationResponse> registerPushToken(
            @CurrentUser UUID userId,
            @Valid @RequestBody PushTokenRequest request
    ) {
        mobilePushService.registerToken(userId, request.getToken());

        return ResponseEntity.ok(
                NotificationResponse.builder()
                        .status(200)
                        .message("Push token registered successfully")
                        .build()
        );
    }

    @DeleteMapping("/push-token")
    public ResponseEntity<NotificationResponse> removePushToken(
            @CurrentUser UUID userId,
            @Valid @RequestBody PushTokenRequest request
    ) {
        mobilePushService.removeToken(userId, request.getToken());

        return ResponseEntity.ok(
                NotificationResponse.builder()
                        .status(200)
                        .message("Push token removed successfully")
                        .build()
        );
    }
}
