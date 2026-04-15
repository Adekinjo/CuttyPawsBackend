package com.cuttypaws.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginHistoryDto {

    private Long id;
    private String email;
    private String status;
    private String ipAddress;
    private String userAgent;
    private String deviceInfo;
    private String failureReason;
    private LocalDateTime createdAt;
}