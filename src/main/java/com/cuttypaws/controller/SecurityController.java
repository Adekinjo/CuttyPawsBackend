package com.cuttypaws.controller;

import com.cuttypaws.entity.SecurityEvent;
import com.cuttypaws.security.SecurityService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/admin/security")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
@RequiredArgsConstructor
public class SecurityController {

    private final SecurityService securityService;

    @GetMapping("/events")
    public List<SecurityEvent> getSecurityEvents() {
        return securityService.getSecurityEvents();
    }

    @GetMapping("/events/ip/{ipAddress}")
    public List<SecurityEvent> getEventsByIp(@PathVariable String ipAddress) {
        return securityService.getEventsByIp(ipAddress);
    }

    @PostMapping("/events/{eventId}/resolve")
    public String resolveEvent(@PathVariable Long eventId) {
        securityService.resolveEvent(eventId);
        return "Event resolved";
    }

    @PostMapping("/block-ip/{ipAddress}")
    public String blockIp(@PathVariable String ipAddress) {
        securityService.blockIp(ipAddress);
        securityService.logSecurityEvent(
                "IP_MANUALLY_BLOCKED",
                "IP manually blocked by admin",
                ipAddress,
                "admin"
        );
        return "IP " + ipAddress + " blocked successfully";
    }

    @PostMapping("/unblock-ip/{ipAddress}")
    public String unblockIp(@PathVariable String ipAddress) {
        securityService.unblockIp(ipAddress);
        securityService.logSecurityEvent(
                "IP_MANUALLY_UNBLOCKED",
                "IP manually unblocked by admin",
                ipAddress,
                "admin"
        );
        return "IP " + ipAddress + " unblocked successfully";
    }

    @GetMapping("/blocked-ips")
    public List<String> getBlockedIps() {
        return securityService.getBlockedIPs();
    }

    @GetMapping("/malicious-users")
    public List<Map<String, Object>> getMaliciousUsers() {
        return securityService.getMaliciousUsers();
    }

    @PostMapping("/block-user-ips/{email}")
    public String blockAllUserIps(@PathVariable String email) {
        List<Map<String, Object>> maliciousUsers = securityService.getMaliciousUsers();

        for (Map<String, Object> user : maliciousUsers) {
            if (email.equals(user.get("email"))) {
                Set<String> ipAddresses = (Set<String>) user.get("ipAddresses");
                for (String ip : ipAddresses) {
                    securityService.blockIp(ip);
                    securityService.logSecurityEvent(
                            "USER_IPS_BLOCKED",
                            "All IPs blocked for malicious user: " + email,
                            ip,
                            "admin"
                    );
                }
                return "Blocked " + ipAddresses.size() + " IPs for user: " + email;
            }
        }

        return "User not found or no malicious activity";
    }
}