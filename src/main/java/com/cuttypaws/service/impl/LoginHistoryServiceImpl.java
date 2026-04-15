package com.cuttypaws.service.impl;

import com.cuttypaws.dto.LoginHistoryDto;
import com.cuttypaws.entity.LoginHistory;
import com.cuttypaws.entity.User;
import com.cuttypaws.exception.NotFoundException;
import com.cuttypaws.repository.LoginHistoryRepo;
import com.cuttypaws.repository.UserRepo;
import com.cuttypaws.service.interf.LoginHistoryService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoginHistoryServiceImpl implements LoginHistoryService {

    private final LoginHistoryRepo loginHistoryRepo;
    private final UserRepo userRepo;

    @Override
    public void saveSuccess(User user, HttpServletRequest request) {
        try {
            LoginHistory history = LoginHistory.builder()
                    .user(user)
                    .email(user.getEmail())
                    .status("SUCCESS")
                    .ipAddress(getClientIp(request))
                    .userAgent(getUserAgent(request))
                    .deviceInfo(buildDeviceInfo(request))
                    .failureReason(null)
                    .createdAt(LocalDateTime.now())
                    .build();

            loginHistoryRepo.save(history);
        } catch (Exception e) {
            log.warn("Could not save successful login history for {}: {}", user.getEmail(), e.getMessage());
        }
    }

    @Override
    public void saveFailed(String email, String failureReason, HttpServletRequest request) {
        try {
            User user = null;

            if (StringUtils.hasText(email)) {
                user = userRepo.findByEmail(email).orElse(null);
            }

            LoginHistory history = LoginHistory.builder()
                    .user(user)
                    .email(email != null ? email : "UNKNOWN")
                    .status("FAILED")
                    .ipAddress(getClientIp(request))
                    .userAgent(getUserAgent(request))
                    .deviceInfo(buildDeviceInfo(request))
                    .failureReason(failureReason)
                    .createdAt(LocalDateTime.now())
                    .build();

            loginHistoryRepo.save(history);
        } catch (Exception e) {
            log.warn("Could not save failed login history for {}: {}", email, e.getMessage());
        }
    }

    @Override
    public List<LoginHistoryDto> getMyLoginHistory() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found"));

        return loginHistoryRepo.findTop20ByUserOrderByCreatedAtDesc(user)
                .stream()
                .map(item -> LoginHistoryDto.builder()
                        .id(item.getId())
                        .email(item.getEmail())
                        .status(item.getStatus())
                        .ipAddress(item.getIpAddress())
                        .userAgent(item.getUserAgent())
                        .deviceInfo(item.getDeviceInfo())
                        .failureReason(item.getFailureReason())
                        .createdAt(item.getCreatedAt())
                        .build())
                .toList();
    }

    private String getClientIp(HttpServletRequest request) {
        if (request == null) return null;

        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(xRealIp)) {
            return xRealIp.trim();
        }

        return request.getRemoteAddr();
    }

    private String getUserAgent(HttpServletRequest request) {
        if (request == null) return null;
        return request.getHeader("User-Agent");
    }

    private String buildDeviceInfo(HttpServletRequest request) {
        String userAgent = getUserAgent(request);
        if (!StringUtils.hasText(userAgent)) {
            return "Unknown device";
        }

        return userAgent.length() > 255 ? userAgent.substring(0, 255) : userAgent;
    }
}