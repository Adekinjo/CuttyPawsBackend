package com.cuttypaws.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

@Component
@RequiredArgsConstructor
public class SecurityFilter extends OncePerRequestFilter {

    private final SecurityService securityService;
    private final RedisRateLimitService rateLimitService;
    private final RedisRateLimitService redisRateLimitService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String clientIP = securityService.getClientIP(request);
        String path = request.getRequestURI();

        // 1) Blocked IP check (keep yours for now)
        if (securityService.isIpBlocked(clientIP)) {
            securityService.logSecurityEvent(
                    "BLOCKED_IP_ACCESS",
                    "Blocked IP attempted to access: " + path,
                    clientIP,
                    getCurrentUserEmail()
            );
            response.sendError(403, "IP address blocked");
            return;
        }

        // 2) Rate-limit sensitive endpoints (production ready via Redis)
        if (path.equals("/auth/login")) {
            boolean allowed = rateLimitService.allow("login:ip:" + clientIP, 8, Duration.ofMinutes(10));
            if (!allowed) {
                securityService.logSecurityEvent("RATE_LIMIT",
                        "Too many login attempts from IP",
                        clientIP,
                        getCurrentUserEmail());
                response.sendError(429, "Too many login attempts. Try again later.");
                return;
            }
        }

        if (path.equals("/auth/register")) {
            boolean allowed = rateLimitService.allow("register:ip:" + clientIP, 5, Duration.ofHours(1));
            if (!allowed) {
                securityService.logSecurityEvent("RATE_LIMIT",
                        "Too many registrations from IP",
                        clientIP,
                        "unknown");
                response.sendError(429, "Too many registration attempts. Try again later.");
                return;
            }
        }

        if (path.startsWith("/service-reviews/") && "POST".equalsIgnoreCase(request.getMethod())) {
            String userEmail = null;

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()) {
                userEmail = auth.getName();
            }

            boolean allowedByIp = redisRateLimitService.allow(
                    "review:ip:" + clientIP,
                    20,
                    java.time.Duration.ofHours(1)
            );

            boolean allowedByUser = true;
            if (userEmail != null) {
                allowedByUser = redisRateLimitService.allow(
                        "review:user:" + userEmail,
                        10,
                        java.time.Duration.ofHours(1)
                );
            }

            if (!allowedByIp || !allowedByUser) {
                response.sendError(429, "Too many review submissions. Try again later.");
                return;
            }
        }

        // 3) Optional: keep your malicious pattern checks OR remove later
        String queryString = request.getQueryString();
        if (securityService.isMaliciousInput(queryString)) {
            securityService.logSecurityEvent("MALICIOUS_URL",
                    "Malicious input in URL: " + queryString,
                    clientIP,
                    getCurrentUserEmail());
        }

        String userAgent = request.getHeader("User-Agent");
        if (securityService.isMaliciousInput(userAgent)) {
            securityService.logSecurityEvent("MALICIOUS_USER_AGENT",
                    "Malicious User-Agent: " + userAgent,
                    clientIP,
                    getCurrentUserEmail());
        }

        filterChain.doFilter(request, response);
    }

    private String getCurrentUserEmail() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated() &&
                    !(authentication instanceof AnonymousAuthenticationToken)) {
                return authentication.getName();
            }
        } catch (Exception ignored) {}
        return "unknown";
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();

        // Always skip preflight
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true;

        // Skip public endpoints
        if (path.startsWith("/auth/")) return true;
        if (path.startsWith("/error")) return true;

        // Skip static
        return path.startsWith("/css/")
                || path.startsWith("/js/")
                || path.startsWith("/images/")
                || path.startsWith("/fonts/")
                || path.endsWith(".ico")
                || path.endsWith(".png")
                || path.endsWith(".jpg")
                || path.endsWith(".jpeg")
                || path.endsWith(".webp")
                || path.endsWith(".svg");
    }


}
