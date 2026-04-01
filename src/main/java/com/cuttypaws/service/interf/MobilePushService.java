package com.cuttypaws.service.interf;

import com.cuttypaws.dto.NotificationDto;

import java.util.UUID;

public interface MobilePushService {

    void registerToken(UUID userId, String token);

    void removeToken(UUID userId, String token);

    void sendNotificationToUser(UUID userId, NotificationDto notification);
}
