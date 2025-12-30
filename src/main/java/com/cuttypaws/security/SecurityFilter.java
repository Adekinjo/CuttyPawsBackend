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

@Component
@RequiredArgsConstructor
public class SecurityFilter extends OncePerRequestFilter {

    private final SecurityService securityService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String clientIP = securityService.getClientIP(request);

        // Check if IP is blocked
        if (securityService.isIpBlocked(clientIP)) {
            securityService.logSecurityEvent(
                    "BLOCKED_IP_ACCESS",
                    "Blocked IP attempted to access: " + request.getRequestURI(),
                    clientIP,
                    getCurrentUserEmail() // Use the method to get user email
            );
            response.sendError(403, "IP address blocked");
            return;
        }

        // Check URL parameters for malicious input
        String queryString = request.getQueryString();
        if (securityService.isMaliciousInput(queryString)) {
            securityService.logSecurityEvent(
                    "MALICIOUS_URL",
                    "Malicious input in URL: " + queryString,
                    clientIP,
                    getCurrentUserEmail() // Use the method to get user email
            );
        }

        // Check headers for malicious input
        String userAgent = request.getHeader("User-Agent");
        if (securityService.isMaliciousInput(userAgent)) {
            securityService.logSecurityEvent(
                    "MALICIOUS_USER_AGENT",
                    "Malicious User-Agent: " + userAgent,
                    clientIP,
                    getCurrentUserEmail() // Use the method to get user email
            );
        }

        filterChain.doFilter(request, response);
    }

    // Helper method to get current user email
    private String getCurrentUserEmail() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated() &&
                    !(authentication instanceof AnonymousAuthenticationToken)) {
                return authentication.getName(); // This will be the email from JWT
            }
        } catch (Exception e) {
            // Log error but don't break the security filter
            System.out.println("Error getting user email: " + e.getMessage());
        }
        return "unknown";
    }

    // Skip security for login/register to avoid issues
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.equals("/auth/login") ||
                path.equals("/auth/register") ||
                path.startsWith("/css/") ||
                path.startsWith("/js/");
    }
}

