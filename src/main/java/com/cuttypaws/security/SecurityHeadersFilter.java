//package com.kinjo.Beauthrist_Backend.security;
//
//import jakarta.servlet.FilterChain;
//import jakarta.servlet.ServletException;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//import org.springframework.stereotype.Component;
//import org.springframework.web.filter.OncePerRequestFilter;
//
//import java.io.IOException;
//
//@Component
//public class SecurityHeadersFilter extends OncePerRequestFilter {
//    @Override
//    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
//                                    FilterChain filterChain) throws IOException, ServletException {
//
//        // Add basic security headers
//        response.setHeader("X-Content-Type-Options", "nosniff");
//        response.setHeader("X-Frame-Options", "DENY");
//        response.setHeader("X-XSS-Protection", "1; mode=block");
//
//        filterChain.doFilter(request, response);
//    }
//}







package com.cuttypaws.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Component
public class SecurityHeadersFilter extends OncePerRequestFilter {

    private static final List<String> NIGERIAN_PAYMENT_PROCESSORS = Arrays.asList(
            "paystack.com", "flutterwave.com", "monnify.com", "remita.net",
            "interswitch.com", "verveinternational.com"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws IOException, ServletException {

        // Generate nonce for CSP (secure alternative to unsafe-inline)
        String nonce = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        request.setAttribute("cspNonce", nonce);

        // Set headers that WORK for Nigeria without compromising security
        setNigerianCompatibleHeaders(response, request, nonce);

        filterChain.doFilter(request, response);
    }

    private void setNigerianCompatibleHeaders(HttpServletResponse response,
                                              HttpServletRequest request,
                                              String nonce) {

        // ✅ CORE SECURITY HEADERS (Essential & compatible)
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "SAMEORIGIN"); // For payment iframes
        response.setHeader("X-XSS-Protection", "1; mode=block");

        // ✅ NIGERIAN-COMPATIBLE CSP
        String csp = buildNigerianCompatibleCSP(request, nonce);
        response.setHeader("Content-Security-Policy", csp);

        // ✅ SMART HSTS - Long but with careful configuration
        if (!isDevelopmentRequest(request)) {
            response.setHeader("Strict-Transport-Security",
                    "max-age=31536000; includeSubDomains"); // 1 year - standard practice
        }

        // ✅ PERMISSIONS POLICY - Nigerian e-commerce features
        response.setHeader("Permissions-Policy",
                "geolocation=(self), " +           // Delivery tracking
                        "camera=(self), " +                // KYC verification
                        "payment=(self), " +               // Payment processing
                        "microphone=(), " +                // Not needed
                        "interest-cohort=()");             // Privacy

        // ✅ CROSS-ORIGIN POLICIES - Balanced approach
        response.setHeader("Cross-Origin-Opener-Policy", "same-origin");
        response.setHeader("Cross-Origin-Resource-Policy", "same-origin");

        // ✅ REFERRER POLICY - Standard
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");

        // Remove server info
        response.setHeader("Server", "KinjoMarket");
    }

    private String buildNigerianCompatibleCSP(HttpServletRequest request, String nonce) {
        String origin = request.getHeader("Origin");
        boolean isDevelopment = isDevelopmentEnvironment(origin);

        if (isDevelopment) {
            // 🔧 DEVELOPMENT - Functional but secure
            return "default-src 'self' https://localhost:9494; " +
                    "script-src 'self' 'nonce-" + nonce + "' https://localhost:9494 https://js.stripe.com; " +
                    "style-src 'self' 'nonce-" + nonce + "' https://localhost:9494 https://fonts.googleapis.com; " +
                    "font-src 'self' https://localhost:9494 https://fonts.gstatic.com data:; " +
                    "img-src 'self' https://localhost:9494 data: blob: https:; " + // HTTPS ONLY
                    "connect-src 'self' https://localhost:9393 https://localhost:9494 wss://localhost:9494; " +
                    "frame-src 'self' https://js.stripe.com https://checkout.stripe.com https://standard.paystack.co https://checkout.flutterwave.com; " +
                    "frame-ancestors 'self'; " +
                    "base-uri 'self'; " +
                    "form-action 'self' https://localhost:9393;";
        }

        // 🌍 PRODUCTION - NIGERIA COMPATIBLE & SECURE
        return "default-src 'self' " +
                "https://www.kinjomarket.com https://www.kinjomarket.com.ng; " +

                // Script sources - Nonce-based (secure) + Nigerian payments
                "script-src 'self' 'nonce-" + nonce + "' " +
                "https://www.kinjomarket.com https://www.kinjomarket.com.ng " +
                "https://js.paystack.co https://standard.paystack.co " +
                "https://checkout.flutterwave.com " +
                "https://monnify.com " +
                "https://remita.net " +
                "https://js.stripe.com https://checkout.stripe.com; " +

                // Style sources - Nonce-based + CDNs
                "style-src 'self' 'nonce-" + nonce + "' " +
                "https://www.kinjomarket.com https://www.kinjomarket.com.ng " +
                "https://fonts.googleapis.com " +
                "https://checkout.flutterwave.com https://standard.paystack.com; " +

                // Font sources
                "font-src 'self' " +
                "https://www.kinjomarket.com https://www.kinjomarket.com.ng " +
                "https://fonts.gstatic.com data: " +
                "https://checkout.flutterwave.com https://standard.paystack.com; " +

                // Image sources - HTTPS ONLY (Nigerian networks support HTTPS)
                "img-src 'self' " +
                "https://www.kinjomarket.com https://www.kinjomarket.com.ng " +
                "data: blob: https:; " + // ✅ NO HTTP - this actually helps Nigerian users

                // Connect sources - All Nigerian APIs
                "connect-src 'self' " +
                "https://www.kinjomarket.com https://www.kinjomarket.com.ng " +
                "https://kinjomarket-backend.onrender.com " +
                "https://api.paystack.co " +
                "https://api.flutterwave.com " +
                "https://api.monnify.com " +
                "https://login.remita.net " +
                "https://api.stripe.com " +
                "wss://kinjomarket-backend.onrender.com; " +

                // Frame sources - Nigerian payment iframes
                "frame-src 'self' " +
                "https://standard.paystack.co " +
                "https://checkout.flutterwave.com " +
                "https://monnify.com " +
                "https://remita.net " +
                "https://js.stripe.com https://checkout.stripe.com; " +

                "frame-ancestors 'self' " +
                "https://www.kinjomarket.com https://www.kinjomarket.com.ng; " +

                "base-uri 'self'; " +

                // Form actions - Nigerian payment endpoints
                "form-action 'self' " +
                "https://www.kinjomarket.com https://www.kinjomarket.com.ng " +
                "https://kinjomarket-backend.onrender.com " +
                "https://standard.paystack.co " +
                "https://checkout.flutterwave.com " +
                "https://monnify.com " +
                "https://remita.net;";
    }

    private boolean isDevelopmentEnvironment(String origin) {
        return origin != null && (
                origin.contains("localhost") ||
                        origin.contains("127.0.0.1") ||
                        origin.contains(":3000") ||
                        origin.contains(":9494") ||
                        origin.contains(".local")
        );
    }

    private boolean isDevelopmentRequest(HttpServletRequest request) {
        String origin = request.getHeader("Origin");
        String host = request.getHeader("Host");

        return (origin != null && isDevelopmentEnvironment(origin)) ||
                (host != null && (host.contains("localhost") || host.contains("127.0.0.1")));
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Skip security headers for static resources
        String path = request.getRequestURI();
        return path.startsWith("/css/") ||
                path.startsWith("/js/") ||
                path.startsWith("/images/") ||
                path.startsWith("/fonts/") ||
                path.endsWith(".ico") ||
                path.endsWith(".png") ||
                path.endsWith(".jpg");
    }
}