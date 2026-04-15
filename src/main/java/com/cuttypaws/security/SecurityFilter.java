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
    private final RedisRateLimitService redisRateLimitService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String clientIP = securityService.getClientIP(request);
        String path = request.getRequestURI();
        String method = request.getMethod();
        String currentUserEmail = getCurrentUserEmail();

        // 1) Hard block
        if (securityService.isIpBlocked(clientIP)) {
            securityService.logSecurityEvent(
                    "BLOCKED_IP_ACCESS",
                    "Blocked IP attempted to access: " + path,
                    clientIP,
                    currentUserEmail
            );
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "IP address blocked");
            return;
        }

        // 2) Rate-limit login
        if ("/auth/login".equals(path) && "POST".equalsIgnoreCase(method)) {
            boolean allowed = redisRateLimitService.allow("login:ip:" + clientIP, 8, Duration.ofMinutes(10));
            if (!allowed) {
                securityService.logSecurityEvent(
                        "RATE_LIMIT_LOGIN",
                        "Too many login attempts from IP",
                        clientIP,
                        currentUserEmail
                );
                response.sendError(429, "Too many login attempts. Try again later.");
                return;
            }
        }

        // 3) Rate-limit registration
        if ("/auth/register".equals(path) && "POST".equalsIgnoreCase(method)) {
            boolean allowed = redisRateLimitService.allow("register:ip:" + clientIP, 5, Duration.ofHours(1));
            if (!allowed) {
                securityService.logSecurityEvent(
                        "RATE_LIMIT_REGISTER",
                        "Too many registrations from IP",
                        clientIP,
                        currentUserEmail
                );
                response.sendError(429, "Too many registration attempts. Try again later.");
                return;
            }
        }

        // 4) Rate-limit password reset
        if ("/auth/request-password-reset".equals(path) && "POST".equalsIgnoreCase(method)) {
            boolean allowedByIp = redisRateLimitService.allow("pwdreset:ip:" + clientIP, 5, Duration.ofMinutes(30));
            boolean allowedByUser = currentUserEmail.equals("unknown")
                    || redisRateLimitService.allow("pwdreset:user:" + currentUserEmail, 3, Duration.ofMinutes(30));

            if (!allowedByIp || !allowedByUser) {
                securityService.logSecurityEvent(
                        "RATE_LIMIT_PASSWORD_RESET",
                        "Too many password reset attempts",
                        clientIP,
                        currentUserEmail
                );
                response.sendError(429, "Too many password reset attempts. Try again later.");
                return;
            }
        }

        // 5) Rate-limit review submissions
        if (path.startsWith("/service-reviews/") && "POST".equalsIgnoreCase(method)) {
            boolean allowedByIp = redisRateLimitService.allow("review:ip:" + clientIP, 20, Duration.ofHours(1));

            boolean allowedByUser = true;
            if (!"unknown".equals(currentUserEmail)) {
                allowedByUser = redisRateLimitService.allow("review:user:" + currentUserEmail, 10, Duration.ofHours(1));
            }

            if (!allowedByIp || !allowedByUser) {
                securityService.logSecurityEvent(
                        "RATE_LIMIT_REVIEW",
                        "Too many review submissions",
                        clientIP,
                        currentUserEmail
                );
                response.sendError(429, "Too many review submissions. Try again later.");
                return;
            }
        }

        // 6) Suspicious URL pattern detection
        String queryString = request.getQueryString();
        if (securityService.isMaliciousInput(queryString)) {
            securityService.logSecurityEvent(
                    "MALICIOUS_URL",
                    "Suspicious input detected in query string",
                    clientIP,
                    currentUserEmail
            );
        }

        // 7) Suspicious User-Agent detection
        String userAgent = request.getHeader("User-Agent");
        if (securityService.isMaliciousInput(userAgent)) {
            securityService.logSecurityEvent(
                    "MALICIOUS_USER_AGENT",
                    "Suspicious User-Agent detected",
                    clientIP,
                    currentUserEmail
            );
        }

        filterChain.doFilter(request, response);
    }

    private String getCurrentUserEmail() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null
                    && authentication.isAuthenticated()
                    && !(authentication instanceof AnonymousAuthenticationToken)) {
                return authentication.getName();
            }
        } catch (Exception ignored) {
        }
        return "unknown";
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();

        if ("OPTIONS".equalsIgnoreCase(method)) {
            return true;
        }

        // Skip only static assets and common framework files
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