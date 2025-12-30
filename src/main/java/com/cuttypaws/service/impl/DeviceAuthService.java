package com.cuttypaws.service.impl;

import com.cuttypaws.entity.*;
import com.cuttypaws.repository.*;
import com.cuttypaws.service.EmailService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class DeviceAuthService {

    private final DeviceVerificationRepo deviceVerificationRepo;
    private final EmailService emailService;

    // In-memory store for attempt tracking to avoid database hits
    private final ConcurrentHashMap<String, Integer> attemptTracker = new ConcurrentHashMap<>();

    // Constants
    private static final int CODE_EXPIRY_MINUTES = 5;
    private static final int MAX_ATTEMPTS = 5;
    private static final int CODE_LENGTH = 6;

    public static class VerifyResult {
        private final boolean success;
        private final String message;
        private final int remainingAttempts;
        private final String remainingTime;

        public VerifyResult(boolean success, String message, int remainingAttempts, String remainingTime) {
            this.success = success;
            this.message = message;
            this.remainingAttempts = remainingAttempts;
            this.remainingTime = remainingTime;
        }

        // Getters
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public int getRemainingAttempts() { return remainingAttempts; }
        public String getRemainingTime() { return remainingTime; }
    }

    public String generateDeviceId(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        String ip = getClientIP(request);
        return String.valueOf((userAgent + ip).hashCode());
    }

    @Transactional
    public void sendVerificationCode(String email, String deviceId) {
        try {
            log.info("Sending verification code for email: {}, deviceId: {}", email, deviceId);

            // Clean up expired verifications first
            deviceVerificationRepo.deleteByExpiryDateBefore(LocalDateTime.now());

            // Check if there's an existing active code
            Optional<DeviceVerification> existingVerification =
                    deviceVerificationRepo.findByEmailAndDeviceIdAndExpiryDateAfter(
                            email, deviceId, LocalDateTime.now());

            if (existingVerification.isPresent()) {
                log.info("Active verification code already exists for: {}", email);
                return; // Don't send new code if active one exists
            }

            // Delete any old codes for this device
            deviceVerificationRepo.deleteByEmailAndDeviceId(email, deviceId);

            // Generate and save new code
            String code = generateVerificationCode();
            LocalDateTime expiryDate = LocalDateTime.now().plusMinutes(CODE_EXPIRY_MINUTES);

            DeviceVerification verification = new DeviceVerification();
            verification.setEmail(email);
            verification.setDeviceId(deviceId);
            verification.setVerificationCode(code);
            verification.setExpiryDate(expiryDate);
            verification.setVerified(false);
            verification.setAttemptCount(0);
            verification.setCreatedAt(LocalDateTime.now());

            deviceVerificationRepo.save(verification);

            // Reset attempt tracker for this verification
            String attemptKey = email + "|" + deviceId;
            attemptTracker.put(attemptKey, 0);

            // Send email
            sendVerificationEmail(email, code);

            log.info("Verification code sent successfully to: {}", email);

        } catch (Exception e) {
            log.error("Failed to send verification code to {}: {}", email, e.getMessage(), e);
            throw new RuntimeException("Failed to send verification code. Please try again.");
        }
    }

    @Transactional
    public VerifyResult verifyDeviceCode(String email, String deviceId, String inputCode) {
        try {
            log.info("Verifying code for email: {}, deviceId: {}", email, deviceId);

            String attemptKey = email + "|" + deviceId;
            int currentAttempts = attemptTracker.getOrDefault(attemptKey, 0);

            // Check if max attempts reached
            if (currentAttempts >= MAX_ATTEMPTS) {
                log.warn("Max verification attempts reached for: {}", email);
                // Clean up the verification record
                deviceVerificationRepo.deleteByEmailAndDeviceId(email, deviceId);
                attemptTracker.remove(attemptKey);
                return new VerifyResult(false,
                        "Too many failed attempts. Please request a new verification code.",
                        0, "00:00");
            }

            // Find active verification
            Optional<DeviceVerification> verificationOpt =
                    deviceVerificationRepo.findByEmailAndDeviceIdAndExpiryDateAfter(
                            email, deviceId, LocalDateTime.now());

            if (verificationOpt.isEmpty()) {
                return new VerifyResult(false,
                        "No active verification code found. Please request a new one.",
                        MAX_ATTEMPTS - currentAttempts, "00:00");
            }

            DeviceVerification verification = verificationOpt.get();
            String remainingTime = getRemainingTime(verification.getExpiryDate());

            // Verify the code
            if (verification.getVerificationCode().equals(inputCode.trim())) {
                // Success - mark as verified
                verification.setVerified(true);
                verification.setVerifiedAt(LocalDateTime.now());
                deviceVerificationRepo.save(verification);

                // Clean up attempt tracker
                attemptTracker.remove(attemptKey);

                log.info("Device verification successful for: {}", email);
                return new VerifyResult(true, "Verification successful",
                        MAX_ATTEMPTS - currentAttempts, remainingTime);
            } else {
                // Wrong code - increment attempts
                currentAttempts++;
                attemptTracker.put(attemptKey, currentAttempts);
                verification.setAttemptCount(currentAttempts);
                deviceVerificationRepo.save(verification);

                int remainingAttempts = MAX_ATTEMPTS - currentAttempts;
                String message = remainingAttempts > 0 ?
                        String.format("Invalid verification code. %d attempt(s) remaining.", remainingAttempts) :
                        "Too many failed attempts. Please request a new verification code.";

                log.warn("Wrong verification code attempt {} for: {}", currentAttempts, email);
                return new VerifyResult(false, message, remainingAttempts, remainingTime);
            }

        } catch (Exception e) {
            log.error("Error during code verification for {}: {}", email, e.getMessage(), e);
            return new VerifyResult(false, "Verification failed. Please try again.", 0, "00:00");
        }
    }

    public boolean isDeviceVerified(String email, String deviceId) {
        try {
            Optional<DeviceVerification> verification =
                    deviceVerificationRepo.findByEmailAndDeviceIdAndVerifiedTrue(email, deviceId);
            return verification.isPresent();
        } catch (Exception e) {
            log.error("Error checking device verification: {}", e.getMessage());
            return false;
        }
    }

    public String getRemainingTime(String email, String deviceId) {
        Optional<DeviceVerification> verification =
                deviceVerificationRepo.findByEmailAndDeviceIdAndExpiryDateAfter(
                        email, deviceId, LocalDateTime.now());

        if (verification.isPresent()) {
            return getRemainingTime(verification.get().getExpiryDate());
        }
        return "00:00";
    }

    private String getRemainingTime(LocalDateTime expiryDate) {
        Duration remaining = Duration.between(LocalDateTime.now(), expiryDate);
        if (remaining.isNegative() || remaining.isZero()) {
            return "00:00";
        }

        long minutes = remaining.toMinutes();
        long seconds = remaining.minusMinutes(minutes).getSeconds();
        return String.format("%02d:%02d", minutes, seconds);
    }

    private String generateVerificationCode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }

    private void sendVerificationEmail(String email, String code) {
        String subject = "Your KinjoMarket Verification Code";
        String body = String.format(
                "Your verification code is: %s\n\n" +
                        "This code will expire in %d minutes.\n\n" +
                        "If you didn't request this code, please ignore this email.\n\n" +
                        "Best regards,\n" +
                        "KinjoMarket Team",
                code, CODE_EXPIRY_MINUTES
        );

        emailService.sendEmail(email, subject, body);
    }

    public String getDeviceInfo(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        String browser = extractBrowser(userAgent);
        String os = extractOS(userAgent);

        return String.format("%s on %s", browser, os);
    }

    public void sendLoginNotification(User user, String deviceInfo, String location, HttpServletRequest request) {
        try {
            String subject = "New Login to Your KinjoMarket Account";
            String body = createLoginNotificationBody(user, deviceInfo, location, request);

            emailService.sendEmail(user.getEmail(), subject, body);
            log.info("Login notification sent to: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send login notification: {}", e.getMessage());
            // Don't throw exception - login notification failure shouldn't block login
        }
    }

    private String createLoginNotificationBody(User user, String deviceInfo, String location, HttpServletRequest request) {
        return String.format(
                "Hello %s,\n\n" +
                        "A new login was detected on your KinjoMarket account:\n\n" +
                        "📱 Device: %s\n" +
                        "📍 Location: %s\n" +
                        "🌐 IP Address: %s\n" +
                        "⏰ Time: %s\n\n" +
                        "If this was you, you can safely ignore this message.\n\n" +
                        "If you don't recognize this activity, please:\n" +
                        "• Change your password immediately\n" +
                        "• Contact our support team\n" +
                        "• Review your account security settings\n\n" +
                        "Stay safe,\n" +
                        "KinjoMarket Security Team",
                user.getName(),
                deviceInfo,
                location,
                getClientIP(request),
                LocalDateTime.now()
        );
    }

    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader != null && !xfHeader.isEmpty()) {
            return xfHeader.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String extractBrowser(String userAgent) {
        if (userAgent == null) return "Unknown Browser";
        if (userAgent.contains("Chrome")) return "Chrome";
        if (userAgent.contains("Firefox")) return "Firefox";
        if (userAgent.contains("Safari") && !userAgent.contains("Chrome")) return "Safari";
        if (userAgent.contains("Edge")) return "Edge";
        if (userAgent.contains("Opera")) return "Opera";
        return "Unknown Browser";
    }

    private String extractOS(String userAgent) {
        if (userAgent == null) return "Unknown OS";
        if (userAgent.contains("Windows")) return "Windows";
        if (userAgent.contains("Mac")) return "macOS";
        if (userAgent.contains("Linux")) return "Linux";
        if (userAgent.contains("Android")) return "Android";
        if (userAgent.contains("iPhone") || userAgent.contains("iPad")) return "iOS";
        return "Unknown OS";
    }
}