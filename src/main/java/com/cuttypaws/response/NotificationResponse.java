package com.cuttypaws.response;

import com.cuttypaws.dto.NotificationDto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class NotificationResponse {

    private int status;
    private String message;

    private LocalDateTime timeStamp;
    private Integer totalComments;
    private Integer totalPages;
    private long totalElement;
    private NotificationDto notification;
    private List<NotificationDto> notificationList;
}
