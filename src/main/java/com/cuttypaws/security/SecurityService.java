package com.cuttypaws.security;

import com.cuttypaws.entity.*;
import com.cuttypaws.repository.*;
import com.cuttypaws.service.EmailService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class SecurityService {

    private final SecurityEventRepo securityEventRepo;
    private final UserRepo userRepo;
    private final EmailService emailService;
    private final SecurityHitCounter hitCounter;

    // Thread-safe IP blocking
    private final Map<String, BlockedIPInfo> blockedIPs = new ConcurrentHashMap<>();

    // Enhanced malicious patterns
    private static final List<String> MALICIOUS_PATTERNS = Arrays.asList(
            "<script", "javascript:", "onload", "onerror", "onclick",
            "select ", "union ", "drop table", "insert into", "update ",
            "delete from", "' or '1'='1", "--", "/*", "*/", "xp_", "exec ",
            "eval(", "document.cookie", "localstorage", "sessionstorage",
            "alert(", "confirm(", "prompt(", "fetch(", "xmlhttprequest"
    );

    /**
     * Enhanced malicious input detection
     */
    public boolean isMaliciousInput(String input) {
        if (input == null || input.trim().isEmpty()) {
            return false;
        }

        String lowerInput = input.toLowerCase();

        for (String pattern : MALICIOUS_PATTERNS) {
            if (lowerInput.contains(pattern)) {
                log.warn("Malicious pattern detected: {} in input: {}", pattern, input);
                return true;
            }
        }

        return false;
    }

    /**
     * Get complete client IP with all headers
     */
    public String getClientIP(HttpServletRequest request) {
        try {
            String[] ipHeaders = {
                    "X-Forwarded-For", "X-Real-IP", "X-Originating-IP",
                    "X-Remote-IP", "X-Remote-Addr", "X-Client-IP",
                    "CF-Connecting-IP", "True-Client-IP", "Proxy-Client-IP",
                    "WL-Proxy-Client-IP", "HTTP_X_FORWARDED_FOR", "HTTP_X_FORWARDED",
                    "HTTP_X_CLUSTER_CLIENT_IP", "HTTP_CLIENT_IP", "HTTP_FORWARDED_FOR",
                    "HTTP_FORWARDED", "HTTP_VIA"
            };

            for (String header : ipHeaders) {
                String ip = request.getHeader(header);
                if (isValidIP(ip)) {
                    return cleanIP(ip);
                }
            }

            return request.getRemoteAddr();

        } catch (Exception e) {
            return "unknown";
        }
    }

    private boolean isValidIP(String ip) {
        return ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip);
    }

    private String cleanIP(String ip) {
        if (ip.contains(",")) {
            return ip.split(",")[0].trim();
        }
        return ip.trim();
    }

    /**
     * Get complete user information if available
     */
    private Map<String, Object> getUserCompleteInfo(String userEmail) {
        Map<String, Object> userInfo = new HashMap<>();

        if (userEmail == null || "unknown".equals(userEmail)) {
            userInfo.put("isRegistered", false);
            userInfo.put("message", "Unregistered Attacker");
            return userInfo;
        }

        try {
            Optional<User> userOpt = userRepo.findByEmail(userEmail);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                userInfo.put("isRegistered", true);
                userInfo.put("id", user.getId());
                userInfo.put("name", user.getName());
                userInfo.put("email", user.getEmail());
                userInfo.put("phoneNumber", user.getPhoneNumber());
                userInfo.put("role", user.getUserRole());
                userInfo.put("companyName", user.getCompanyName());
                userInfo.put("registrationDate", user.getCreatedAt());
                userInfo.put("message", "REGISTERED USER ATTACK");
            } else {
                userInfo.put("isRegistered", false);
                userInfo.put("message", "Unknown Email Attempt");
            }
        } catch (Exception e) {
            userInfo.put("isRegistered", false);
            userInfo.put("message", "Error fetching user data");
        }

        return userInfo;
    }

    /**
     * Get complete IP geolocation information
     */
    private Map<String, String> getCompleteIPLocation(String ipAddress) {
        Map<String, String> location = new HashMap<>();

        try {
            if (isPrivateIP(ipAddress)) {
                location.put("country", "Local Network");
                location.put("city", "Internal System");
                location.put("isp", "Private Network");
                location.put("coordinates", "N/A");
                location.put("timezone", "N/A");
                location.put("status", "Internal");
            } else {
                Map<String, String> realLocation = getRealIPLocation(ipAddress);
                if (realLocation != null && !"Unknown".equals(realLocation.get("country"))) {
                    location.putAll(realLocation);
                    location.put("status", "External - Geolocated");
                } else {
                    location.put("country", "Unknown");
                    location.put("city", "Unknown");
                    location.put("isp", "Unknown");
                    location.put("coordinates", "N/A");
                    location.put("timezone", "N/A");
                    location.put("status", "External - Unknown Location");
                }
            }
        } catch (Exception e) {
            location.put("country", "Lookup Failed");
            location.put("city", "Error");
            location.put("isp", "Error");
            location.put("coordinates", "N/A");
            location.put("timezone", "N/A");
            location.put("status", "Location Service Error");
        }

        return location;
    }

    private boolean isPrivateIP(String ip) {
        return ip.equals("localhost") || ip.equals("127.0.0.1") ||
                ip.equals("0:0:0:0:0:0:0:1") || ip.startsWith("192.168.") ||
                ip.startsWith("10.") || ip.startsWith("172.16.");
    }

    private Map<String, String> getRealIPLocation(String ipAddress) {
        try {
            URL url = new URL("http://ip-api.com/json/" + ipAddress +
                    "?fields=status,message,country,countryCode,region,regionName,city,zip,lat,lon,timezone,isp,org,as,query");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            return parseCompleteLocationResponse(response.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private Map<String, String> parseCompleteLocationResponse(String json) {
        Map<String, String> location = new HashMap<>();

        try {
            if (json.contains("\"status\":\"success\"")) {
                location.put("country", extractJsonValue(json, "country") + " (" + extractJsonValue(json, "countryCode") + ")");
                location.put("city", extractJsonValue(json, "city") + ", " + extractJsonValue(json, "regionName"));
                location.put("isp", extractJsonValue(json, "isp") + " / " + extractJsonValue(json, "org"));
                location.put("coordinates", extractJsonValue(json, "lat") + ", " + extractJsonValue(json, "lon"));
                location.put("timezone", extractJsonValue(json, "timezone"));
                location.put("zip", extractJsonValue(json, "zip"));
            } else {
                location.put("country", "Unknown");
                location.put("city", "Unknown");
                location.put("isp", "Unknown");
                location.put("coordinates", "N/A");
                location.put("timezone", "N/A");
                location.put("zip", "N/A");
            }
        } catch (Exception e) {
            location.put("country", "Parse Error");
            location.put("city", "Parse Error");
            location.put("isp", "Parse Error");
            location.put("coordinates", "N/A");
            location.put("timezone", "N/A");
            location.put("zip", "N/A");
        }

        return location;
    }

    private String extractJsonValue(String json, String field) {
        try {
            String pattern = "\"" + field + "\":\"(.*?)\"";
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher m = p.matcher(json);
            if (m.find()) {
                return m.group(1);
            }

            // Try for numeric values
            pattern = "\"" + field + "\":(.*?),";
            p = java.util.regex.Pattern.compile(pattern);
            m = p.matcher(json);
            if (m.find()) {
                return m.group(1);
            }
        } catch (Exception e) {
            // Ignore errors
        }
        return "Unknown";
    }

    /**
     * MAIN SECURITY EVENT LOGGING - COMPLETE DETAILS
     */
//    @Async
//    @Transactional
//    public void logSecurityEvent(String eventType, String description, String ipAddress, String userEmail) {
//        try {
//            // Get complete user information
//            Map<String, Object> userInfo = getUserCompleteInfo(userEmail);
//
//            // Get complete IP location information
//            Map<String, String> locationInfo = getCompleteIPLocation(ipAddress);
//
//            // Create security event with ALL details
//            SecurityEvent event = createCompleteSecurityEvent(eventType, description, ipAddress, userEmail, userInfo, locationInfo);
//            securityEventRepo.save(event);
//
//            log.info("Complete security event logged: {} - {} - {}", eventType, ipAddress, userEmail);
//
//            // Send COMPLETE alert email
//            sendCompleteSecurityAlert(event, userInfo, locationInfo);
//
//        } catch (Exception e) {
//            log.error("Failed to log complete security event: {}", e.getMessage());
//            createFallbackEvent(eventType, description, ipAddress, userEmail);
//        }
//    }

    @Async
    @Transactional
    public void logSecurityEvent(String eventType, String description, String ipAddress, String userEmail) {
        try {
            // 1) Get user info (cheap)
            Map<String, Object> userInfo = getUserCompleteInfo(userEmail);

            // 2) Save event immediately (DON'T do heavy external calls first)
            // For normal events, avoid geo lookup (expensive + can be slow)
            Map<String, String> placeholderLocation = new HashMap<>();
            placeholderLocation.put("country", "Unknown");
            placeholderLocation.put("city", "Unknown");
            placeholderLocation.put("isp", "Unknown");
            placeholderLocation.put("coordinates", "N/A");
            placeholderLocation.put("timezone", "N/A");
            placeholderLocation.put("status", "Not Looked Up");

            SecurityEvent event = createCompleteSecurityEvent(
                    eventType,
                    description,
                    ipAddress,
                    userEmail,
                    userInfo,
                    placeholderLocation
            );
            securityEventRepo.save(event);

            // 3) Only apply "10 hits → email" to malicious/security events
            if (!isMaliciousType(eventType)) {
                log.info("Security event logged (non-malicious): type={} ip={} email={}", eventType, ipAddress, userEmail);
                return;
            }

            // 4) Redis hit counting within a time window
            // Example: 30-minute rolling window
            // Key includes ip + eventType so "MALICIOUS_URL" is tracked separately from "MALICIOUS_USER_AGENT"
            String counterKey = ipAddress + ":" + eventType;
            long count = hitCounter.incrementWithTtl(counterKey, java.time.Duration.ofMinutes(30));

            log.warn("Malicious hit recorded: type={} ip={} email={} count={}", eventType, ipAddress, userEmail, count);

            // 5) Send email ONLY at exactly count == 10 (prevents spamming)
            if (count == 10) {
                Map<String, String> locationInfo = getCompleteIPLocation(ipAddress);
                sendCompleteSecurityAlert(event, userInfo, locationInfo);
            }

        } catch (Exception e) {
            log.error("Failed to log security event: {}", e.getMessage(), e);
            createFallbackEvent(eventType, description, ipAddress, userEmail);
        }
    }

    private boolean isMaliciousType(String eventType) {
        if (eventType == null) return false;
        String t = eventType.toUpperCase(Locale.ROOT);

        return t.contains("MALICIOUS")
                || t.contains("XSS")
                || t.contains("SQL")
                || t.contains("BRUTE_FORCE")
                || t.contains("BLOCKED")
                || t.contains("RATE_LIMIT");
    }



    private SecurityEvent createCompleteSecurityEvent(String eventType, String description, String ipAddress,
                                                      String userEmail, Map<String, Object> userInfo,
                                                      Map<String, String> locationInfo) {
        SecurityEvent event = new SecurityEvent();
        event.setEventType(eventType);
        event.setDescription(description);
        event.setIpAddress(ipAddress);
        event.setUserEmail(userEmail);
        event.setTimestamp(LocalDateTime.now());
        event.setResolved(false);

        // Store all location information
        event.setCountry(locationInfo.get("country"));
        event.setCity(locationInfo.get("city"));
        event.setIsp(locationInfo.get("isp"));

        // Store additional context in description
        String userContext = (Boolean) userInfo.get("isRegistered") ?
                "REGISTERED USER: " + userInfo.get("name") + " (" + userInfo.get("email") + ")" :
                "UNREGISTERED ATTACKER";

        String locationContext = "LOCATION: " + locationInfo.get("city") + ", " + locationInfo.get("country") +
                " | ISP: " + locationInfo.get("isp") + " | STATUS: " + locationInfo.get("status");

        event.setDescription(description + " | " + userContext + " | " + locationContext);

        return event;
    }

    /**
     * SEND COMPLETE SECURITY ALERT EMAIL
     */
    private void sendCompleteSecurityAlert(SecurityEvent event, Map<String, Object> userInfo, Map<String, String> locationInfo) {
        try {
            String subject = String.format("🚨 SECURITY BREACH: %s from %s",
                    event.getEventType(),
                    userInfo.get("message"));

            String emailBody = createCompleteAlertEmailBody(event, userInfo, locationInfo);

            emailService.sendEmail("adekunlekinjo@gmail.com", subject, emailBody);
            log.info("Complete security alert sent for: {} - {}", event.getEventType(), event.getIpAddress());

        } catch (Exception e) {
            log.error("Failed to send complete security alert: {}", e.getMessage());
        }
    }

    private String createCompleteAlertEmailBody(SecurityEvent event, Map<String, Object> userInfo, Map<String, String> locationInfo) {
        StringBuilder body = new StringBuilder();

        body.append("🚨 COMPLETE SECURITY BREACH DETECTED 🚨\n\n");

        body.append("📋 ATTACK SUMMARY:\n");
        body.append("═══════════════════════════════════════════\n");
        body.append("• Attack Type: ").append(event.getEventType()).append("\n");
        body.append("• Timestamp: ").append(event.getTimestamp()).append("\n");
        body.append("• Description: ").append(event.getDescription()).append("\n\n");

        body.append("👤 ATTACKER IDENTITY:\n");
        body.append("═══════════════════════════════════════════\n");
        if ((Boolean) userInfo.get("isRegistered")) {
            body.append("• STATUS: 🔴 REGISTERED USER ATTACK\n");
            body.append("• User ID: ").append(userInfo.get("id")).append("\n");
            body.append("• Full Name: ").append(userInfo.get("name")).append("\n");
            body.append("• Email: ").append(userInfo.get("email")).append("\n");
            body.append("• Phone: ").append(userInfo.get("phoneNumber")).append("\n");
            body.append("• User Role: ").append(userInfo.get("role")).append("\n");
            if (userInfo.get("companyName") != null) {
                body.append("• Company: ").append(userInfo.get("companyName")).append("\n");
            }
            body.append("• Registered Since: ").append(userInfo.get("registrationDate")).append("\n");
        } else {
            body.append("• STATUS: 🟡 EXTERNAL ATTACKER\n");
            body.append("• Email Used: ").append(event.getUserEmail()).append("\n");
            body.append("• Identity: Unregistered/Unknown User\n");
        }
        body.append("\n");

        body.append("🌍 ATTACKER LOCATION:\n");
        body.append("═══════════════════════════════════════════\n");
        body.append("• IP Address: ").append(event.getIpAddress()).append("\n");
        body.append("• Location: ").append(locationInfo.get("city")).append(", ").append(locationInfo.get("country")).append("\n");
        body.append("• ISP: ").append(locationInfo.get("isp")).append("\n");
        body.append("• Coordinates: ").append(locationInfo.get("coordinates")).append("\n");
        body.append("• Timezone: ").append(locationInfo.get("timezone")).append("\n");
        body.append("• Status: ").append(locationInfo.get("status")).append("\n\n");

        body.append("🛡️ IMMEDIATE ACTIONS REQUIRED:\n");
        body.append("═══════════════════════════════════════════\n");
        if ((Boolean) userInfo.get("isRegistered")) {
            body.append("1. 🚫 SUSPEND user account immediately\n");
            body.append("2. 📞 Contact user for verification\n");
            body.append("3. 🔍 Review user's recent activity\n");
            body.append("4. 📧 Notify security team about internal threat\n");
            body.append("5. 🗑️ Consider account termination if malicious\n");
        } else {
            body.append("1. 🚫 BLOCK IP address immediately\n");
            body.append("2. 🔍 Check for similar attacks from this IP\n");
            body.append("3. 🌐 Monitor for attacks from same region\n");
            body.append("4. ⚙️ Enhance firewall rules if needed\n");
        }
        body.append("6. 📊 Update security protocols\n");
        body.append("7. 🔄 Review and strengthen input validation\n\n");

        body.append("🔍 INVESTIGATION NOTES:\n");
        body.append("═══════════════════════════════════════════\n");
        body.append("• Check security dashboard for full details\n");
        body.append("• Review server logs for related activity\n");
        body.append("• Monitor for pattern repetition\n");
        body.append("• Update threat intelligence database\n\n");

        body.append("⚠️ This is an AUTOMATED CRITICAL SECURITY ALERT\n");
        body.append("   Immediate investigation and action required!\n");

        return body.toString();
    }

    private void createFallbackEvent(String eventType, String description, String ipAddress, String userEmail) {
        try {
            SecurityEvent event = new SecurityEvent();
            event.setEventType(eventType);
            event.setDescription(description);
            event.setIpAddress(ipAddress);
            event.setUserEmail(userEmail);
            event.setTimestamp(LocalDateTime.now());
            event.setResolved(false);
            securityEventRepo.save(event);
        } catch (Exception ex) {
            log.error("Critical: Failed fallback event creation: {}", ex.getMessage());
        }
    }

    // IP Blocking methods (keep existing)
    public List<String> getBlockedIPs() {
        return new ArrayList<>(blockedIPs.keySet());
    }

    public boolean isIpBlocked(String ipAddress) {
        BlockedIPInfo info = blockedIPs.get(ipAddress);
        return info != null && !info.isExpired(LocalDateTime.now());
    }

    public void blockIp(String ipAddress) {
        blockedIPs.put(ipAddress, new BlockedIPInfo(24));
        log.info("IP {} blocked", ipAddress);
    }

    public void unblockIp(String ipAddress) {
        blockedIPs.remove(ipAddress);
        log.info("IP {} unblocked", ipAddress);
    }

    // Security event management (keep existing)
    public List<SecurityEvent> getSecurityEvents() {
        return securityEventRepo.findByResolvedFalseOrderByTimestampDesc();
    }

    @Transactional
    public void resolveEvent(Long eventId) {
        securityEventRepo.findById(eventId).ifPresent(event -> {
            event.setResolved(true);
            securityEventRepo.save(event);
        });
    }

    public List<SecurityEvent> getEventsByIp(String ipAddress) {
        return securityEventRepo.findByIpAddressOrderByTimestampDesc(ipAddress);
    }

    /**
     * Get malicious users with complete information
     */
    public List<Map<String, Object>> getMaliciousUsers() {
        List<SecurityEvent> allEvents = securityEventRepo.findAll();
        Map<String, Map<String, Object>> userMap = new HashMap<>();

        for (SecurityEvent event : allEvents) {
            if (isMaliciousEvent(event) && !"unknown".equals(event.getUserEmail())) {
                String email = event.getUserEmail();
                Map<String, Object> userInfo = userMap.computeIfAbsent(email, k -> {
                    Map<String, Object> newUserInfo = new HashMap<>();
                    newUserInfo.put("email", email);
                    newUserInfo.put("events", new ArrayList<SecurityEvent>());
                    newUserInfo.put("ipAddresses", new HashSet<String>());
                    newUserInfo.put("userDetails", getUserCompleteInfo(email));
                    return newUserInfo;
                });

                ((List<SecurityEvent>) userInfo.get("events")).add(event);
                ((Set<String>) userInfo.get("ipAddresses")).add(event.getIpAddress());
            }
        }

        return finalizeMaliciousUsers(userMap);
    }

    private List<Map<String, Object>> finalizeMaliciousUsers(Map<String, Map<String, Object>> userMap) {
        List<Map<String, Object>> result = new ArrayList<>();

        for (Map<String, Object> userInfo : userMap.values()) {
            List<SecurityEvent> events = (List<SecurityEvent>) userInfo.get("events");
            Set<String> ipAddresses = (Set<String>) userInfo.get("ipAddresses");
            Map<String, Object> userDetails = (Map<String, Object>) userInfo.get("userDetails");

            userInfo.put("eventCount", events.size());
            userInfo.put("ipCount", ipAddresses.size());
            userInfo.put("isRegistered", userDetails.get("isRegistered"));
            userInfo.put("userName", userDetails.get("name"));
            userInfo.put("userRole", userDetails.get("role"));
            userInfo.put("riskLevel", calculateRiskLevel(events.size(), ipAddresses.size()));

            result.add(userInfo);
        }

        result.sort((a, b) -> Integer.compare((int) b.get("riskLevel"), (int) a.get("riskLevel")));
        return result;
    }

    private int calculateRiskLevel(int eventCount, int ipCount) {
        return Math.min(eventCount + (ipCount * 2), 10);
    }

    private boolean isMaliciousEvent(SecurityEvent event) {
        String type = event.getEventType();
        return type.contains("MALICIOUS") || type.contains("XSS") ||
                type.contains("SQL") || type.contains("BLOCKED") || type.contains("BRUTE_FORCE");
    }

    /**
     * Inner class for IP blocking
     */
    private static class BlockedIPInfo {
        private final LocalDateTime blockedAt;
        private final int blockHours;

        public BlockedIPInfo(int blockHours) {
            this.blockedAt = LocalDateTime.now();
            this.blockHours = blockHours;
        }

        public boolean isExpired(LocalDateTime now) {
            return blockedAt.plusHours(blockHours).isBefore(now);
        }
    }
    public String getLocationInfo(String ipAddress) {
        try {
            Map<String, String> location = getCompleteIPLocation(ipAddress);
            if (location != null && !"Local Network".equals(location.get("country"))) {
                return location.get("city") + ", " + location.get("country");
            } else {
                return "Local Network";
            }
        } catch (Exception e) {
            log.error("Error getting location info: {}", e.getMessage());
            return "Unknown Location";
        }
    }

    // Add a simpler method for public use
    public String getSimpleLocation(String ipAddress) {
        try {
            if (isPrivateIP(ipAddress)) {
                return "Local Network";
            }

            Map<String, String> location = getCompleteIPLocation(ipAddress);
            if (location != null && !"Unknown".equals(location.get("country"))) {
                return location.get("city") + ", " + location.get("country");
            }
            return "Unknown Location";
        } catch (Exception e) {
            return "Unknown Location";
        }
    }
}

