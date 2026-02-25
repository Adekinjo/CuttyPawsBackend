//package com.cuttypaws.security;
//
//import jakarta.servlet.FilterChain;
//import jakarta.servlet.ServletException;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//import org.springframework.stereotype.Component;
//import org.springframework.web.filter.OncePerRequestFilter;
//
//import java.io.IOException;
//import java.util.Arrays;
//import java.util.List;
//import java.util.UUID;
//
//@Component
//public class SecurityHeadersFilter extends OncePerRequestFilter {
//
//    private static final List<String> NIGERIAN_PAYMENT_PROCESSORS = Arrays.asList(
//            "paystack.com", "flutterwave.com", "monnify.com", "remita.net",
//            "interswitch.com", "verveinternational.com"
//    );
//
//    @Override
//    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
//                                    FilterChain filterChain) throws IOException, ServletException {
//
//        // Generate nonce for CSP (secure alternative to unsafe-inline)
//        String nonce = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
//        request.setAttribute("cspNonce", nonce);
//
//        // Set headers that WORK for Nigeria without compromising security
//        setNigerianCompatibleHeaders(response, request, nonce);
//
//        filterChain.doFilter(request, response);
//    }
//
//    private void setNigerianCompatibleHeaders(HttpServletResponse response,
//                                              HttpServletRequest request,
//                                              String nonce) {
//
//        // ✅ CORE SECURITY HEADERS (Essential & compatible)
//        response.setHeader("X-Content-Type-Options", "nosniff");
//        response.setHeader("X-Frame-Options", "SAMEORIGIN"); // For payment iframes
//        response.setHeader("X-XSS-Protection", "1; mode=block");
//
//        // ✅ NIGERIAN-COMPATIBLE CSP
//        String csp = buildNigerianCompatibleCSP(request, nonce);
//        response.setHeader("Content-Security-Policy", csp);
//
//        // ✅ SMART HSTS - Long but with careful configuration
//        if (!isDevelopmentRequest(request)) {
//            response.setHeader("Strict-Transport-Security",
//                    "max-age=31536000; includeSubDomains"); // 1 year - standard practice
//        }
//
//        // ✅ PERMISSIONS POLICY - Nigerian e-commerce features
//        response.setHeader("Permissions-Policy",
//                "geolocation=(self), " +           // Delivery tracking
//                        "camera=(self), " +                // KYC verification
//                        "payment=(self), " +               // Payment processing
//                        "microphone=(), " +                // Not needed
//                        "interest-cohort=()");             // Privacy
//
//        // ✅ CROSS-ORIGIN POLICIES - Balanced approach
//        response.setHeader("Cross-Origin-Opener-Policy", "same-origin");
//        response.setHeader("Cross-Origin-Resource-Policy", "same-origin");
//
//        // ✅ REFERRER POLICY - Standard
//        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
//
//        // Remove server info
//        response.setHeader("Server", "CuttyPaws");
//    }
//
//    private String buildNigerianCompatibleCSP(HttpServletRequest request, String nonce) {
//        String origin = request.getHeader("Origin");
//        boolean isDevelopment = isDevelopmentEnvironment(origin);
//
//        if (isDevelopment) {
//            // 🔧 DEVELOPMENT - Functional but secure
//            return "default-src 'self' https://localhost:9494; " +
//                    "script-src 'self' 'nonce-" + nonce + "' https://localhost:9494 https://js.stripe.com; " +
//                    "style-src 'self' 'nonce-" + nonce + "' https://localhost:9494 https://fonts.googleapis.com; " +
//                    "font-src 'self' https://localhost:9494 https://fonts.gstatic.com data:; " +
//                    "img-src 'self' https://localhost:9494 data: blob: https:; " + // HTTPS ONLY
//                    "connect-src 'self' https://localhost:9393 https://localhost:9494 wss://localhost:9494; " +
//                    "frame-src 'self' https://js.stripe.com https://checkout.stripe.com https://standard.paystack.co https://checkout.flutterwave.com; " +
//                    "frame-ancestors 'self'; " +
//                    "base-uri 'self'; " +
//                    "form-action 'self' https://localhost:9393;";
//        }
//
//        // 🌍 PRODUCTION - NIGERIA COMPATIBLE & SECURE
//        return "default-src 'self' " +
//                "https://www.kinjomarket.com https://www.kinjomarket.com.ng; " +
//
//                // Script sources - Nonce-based (secure) + Nigerian payments
//                "script-src 'self' 'nonce-" + nonce + "' " +
//                "https://www.kinjomarket.com https://www.kinjomarket.com.ng " +
//                "https://js.paystack.co https://standard.paystack.co " +
//                "https://checkout.flutterwave.com " +
//                "https://monnify.com " +
//                "https://remita.net " +
//                "https://js.stripe.com https://checkout.stripe.com; " +
//
//                // Style sources - Nonce-based + CDNs
//                "style-src 'self' 'nonce-" + nonce + "' " +
//                "https://www.kinjomarket.com https://www.kinjomarket.com.ng " +
//                "https://fonts.googleapis.com " +
//                "https://checkout.flutterwave.com https://standard.paystack.com; " +
//
//                // Font sources
//                "font-src 'self' " +
//                "https://www.kinjomarket.com https://www.kinjomarket.com.ng " +
//                "https://fonts.gstatic.com data: " +
//                "https://checkout.flutterwave.com https://standard.paystack.com; " +
//
//                // Image sources - HTTPS ONLY (Nigerian networks support HTTPS)
//                "img-src 'self' " +
//                "https://www.kinjomarket.com https://www.kinjomarket.com.ng " +
//                "data: blob: https:; " + // ✅ NO HTTP - this actually helps Nigerian users
//
//                // Connect sources - All Nigerian APIs
//                "connect-src 'self' " +
//                "https://www.kinjomarket.com https://www.kinjomarket.com.ng " +
//                "https://kinjomarket-backend.onrender.com " +
//                "https://api.paystack.co " +
//                "https://api.flutterwave.com " +
//                "https://api.monnify.com " +
//                "https://login.remita.net " +
//                "https://api.stripe.com " +
//                "wss://kinjomarket-backend.onrender.com; " +
//
//                // Frame sources - Nigerian payment iframes
//                "frame-src 'self' " +
//                "https://standard.paystack.co " +
//                "https://checkout.flutterwave.com " +
//                "https://monnify.com " +
//                "https://remita.net " +
//                "https://js.stripe.com https://checkout.stripe.com; " +
//
//                "frame-ancestors 'self' " +
//                "https://www.kinjomarket.com https://www.kinjomarket.com.ng; " +
//
//                "base-uri 'self'; " +
//
//                // Form actions - Nigerian payment endpoints
//                "form-action 'self' " +
//                "https://www.kinjomarket.com https://www.kinjomarket.com.ng " +
//                "https://kinjomarket-backend.onrender.com " +
//                "https://standard.paystack.co " +
//                "https://checkout.flutterwave.com " +
//                "https://monnify.com " +
//                "https://remita.net;";
//    }
//
//    private boolean isDevelopmentEnvironment(String origin) {
//        return origin != null && (
//                origin.contains("localhost") ||
//                        origin.contains("127.0.0.1") ||
//                        origin.contains(":3000") ||
//                        origin.contains(":9494") ||
//                        origin.contains(".local")
//        );
//    }
//
//    private boolean isDevelopmentRequest(HttpServletRequest request) {
//        String origin = request.getHeader("Origin");
//        String host = request.getHeader("Host");
//
//        return (origin != null && isDevelopmentEnvironment(origin)) ||
//                (host != null && (host.contains("localhost") || host.contains("127.0.0.1")));
//    }
//
//    @Override
//    protected boolean shouldNotFilter(HttpServletRequest request) {
//        // Skip security headers for static resources
//        String path = request.getRequestURI();
//        return path.startsWith("/css/") ||
//                path.startsWith("/js/") ||
//                path.startsWith("/images/") ||
//                path.startsWith("/fonts/") ||
//                path.endsWith(".ico") ||
//                path.endsWith(".png") ||
//                path.endsWith(".jpg");
//    }
//}









package com.cuttypaws.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;

@Component
public class SecurityHeadersFilter extends OncePerRequestFilter {

    /**
     * ✅ Put YOUR real domains here.
     * - Frontend: cuttypaws.com (and www)
     * - Backend: api.cuttypaws.com (or your render url)
     * - CDN/S3: if you serve images from S3/CloudFront, add it to IMG/CONNECT as needed
     */
    private static final List<String> FRONTEND_ORIGINS = List.of(
            "https://cuttypaws.com",
            "https://www.cuttypaws.com"
    );

    private static final List<String> BACKEND_ORIGINS = List.of(
            "https://api.cuttypaws.com" ,
             "https://cuttypaws-backend.onrender.com" // example
    );

    /**
     * ✅ Allowed third-party payment providers
     * IMPORTANT: ONLY domains (no query strings in CSP)
     */
    private static final List<String> PAYMENT_SCRIPT_FRAME_ORIGINS = List.of(
            "https://js.paystack.co",
            "https://standard.paystack.co",
            "https://checkout.flutterwave.com",
            "https://monnify.com",
            "https://remita.net",
            "https://js.stripe.com",
            "https://checkout.stripe.com"
    );

    private static final List<String> PAYMENT_API_ORIGINS = List.of(
            "https://api.paystack.co",
            "https://api.flutterwave.com",
            "https://api.monnify.com",
            "https://login.remita.net",
            "https://api.stripe.com"
    );

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws IOException, ServletException {

        // Nonce: short + URL-safe. Use for inline scripts you control (if any).
        String nonce = generateNonce();
        request.setAttribute("cspNonce", nonce);

        setSecurityHeaders(request, response, nonce);

        filterChain.doFilter(request, response);
    }

    private void setSecurityHeaders(HttpServletRequest request, HttpServletResponse response, String nonce) {

        // --- Core hardening ---
        response.setHeader("X-Content-Type-Options", "nosniff");

        // Modern browsers ignore X-XSS-Protection; it's legacy. Setting it doesn't hurt, but don't rely on it.
        response.setHeader("X-XSS-Protection", "0");

        // Clickjacking protection. If you embed your site in other domains, adjust.
        response.setHeader("X-Frame-Options", "SAMEORIGIN");

        // Referrer policy: safe default for web apps
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");

        // Permissions policy: restrict what the browser can use
        response.setHeader("Permissions-Policy",
                "geolocation=(), camera=(), microphone=(), payment=(self)");

        // --- HSTS (ONLY when behind HTTPS in production) ---
        if (!isDevelopmentRequest(request)) {
            // 1 year is normal. Add preload only when you're 100% sure and ready.
            response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
        }

        // --- CSP ---
        // You can first run with Report-Only to test:
        // response.setHeader("Content-Security-Policy-Report-Only", buildCsp(request, nonce));
        response.setHeader("Content-Security-Policy", buildCsp(request, nonce));

        // Remove server banner if your stack adds it (some proxies override this anyway).
        response.setHeader(HttpHeaders.SERVER, "CuttyPaws");
    }

    /**
     * ✅ Production CSP rules
     * - "world accessible" means you do NOT restrict by geography. You only restrict by allowed origins.
     * - Nonce-based script policy: no 'unsafe-inline'
     * - Keep connect-src broad enough for your backend and websockets if you use them.
     */
    private String buildCsp(HttpServletRequest request, String nonce) {
        boolean dev = isDevelopmentRequest(request);

        // Your app origin(s)
        String appOrigins = String.join(" ", FRONTEND_ORIGINS);

        // Backend(s)
        String apiOrigins = String.join(" ", BACKEND_ORIGINS);

        // Payment providers
        String payFramesScripts = String.join(" ", PAYMENT_SCRIPT_FRAME_ORIGINS);
        String payApis = String.join(" ", PAYMENT_API_ORIGINS);

        if (dev) {
            // Dev: allow localhost tooling. Keep it practical.
            return ""
                    + "default-src 'self' http://localhost:3000 http://localhost:5173; "
                    + "base-uri 'self'; "
                    + "object-src 'none'; "
                    + "frame-ancestors 'self'; "
                    + "script-src 'self' 'nonce-" + nonce + "' http://localhost:3000 http://localhost:5173 " + payFramesScripts + "; "
                    + "style-src 'self' 'unsafe-inline' http://localhost:3000 http://localhost:5173 https://fonts.googleapis.com; "
                    + "font-src 'self' https://fonts.gstatic.com data:; "
                    + "img-src 'self' data: blob: https:; "
                    + "connect-src 'self' http://localhost:8080 http://localhost:9393 http://localhost:9494 ws://localhost:* " + payApis + "; "
                    + "frame-src 'self' " + payFramesScripts + "; "
                    + "form-action 'self' http://localhost:* " + payFramesScripts + "; ";
        }

        // Production: strict + global access
        return ""
                + "default-src 'self' " + appOrigins + "; "
                + "base-uri 'self'; "
                + "object-src 'none'; "
                + "frame-ancestors 'self'; "

                // Scripts: nonce-based; allow payment script hosts
                + "script-src 'self' 'nonce-" + nonce + "' " + appOrigins + " " + payFramesScripts + "; "

                // Styles: best is nonce-based too, but most apps use external CSS.
                // If you are using inline styles, you'll need 'unsafe-inline' or add nonce support to styles.
                + "style-src 'self' " + appOrigins + " https://fonts.googleapis.com; "

                + "font-src 'self' https://fonts.gstatic.com data:; "

                // Images: allow https + data/blob (for uploads/previews)
                + "img-src 'self' " + appOrigins + " data: blob: https:; "

                // API calls: your backend + payment APIs
                + "connect-src 'self' " + appOrigins + " " + apiOrigins + " " + payApis + "; "

                // Payments often require iframes
                + "frame-src 'self' " + payFramesScripts + "; "

                // Form posts (if any)
                + "form-action 'self' " + appOrigins + " " + apiOrigins + " " + payFramesScripts + "; ";
    }

    /**
     * Keep these methods intact (you said other classes rely on them).
     */
    public boolean isDevelopmentEnvironment(String origin) {
        return origin != null && (
                origin.contains("localhost") ||
                        origin.contains("127.0.0.1") ||
                        origin.contains(":3000") ||
                        origin.contains(":5173") ||
                        origin.contains(":9494") ||
                        origin.contains(".local")
        );
    }

    public boolean isDevelopmentRequest(HttpServletRequest request) {
        String origin = request.getHeader("Origin");
        String host = request.getHeader("Host");
        return (origin != null && isDevelopmentEnvironment(origin))
                || (host != null && (host.contains("localhost") || host.contains("127.0.0.1")));
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Keep static assets unblocked (optional)
        String path = request.getRequestURI();
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

    private String generateNonce() {
        byte[] bytes = new byte[16];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
