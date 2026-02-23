package com.cuttypaws.security;

import com.cuttypaws.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@Service
@Slf4j
public class JwtUtils {

    // Token expiration times
    private static final long ACCESS_TOKEN_EXPIRY = 1000L * 60L * 15L; // 15 minutes
    private static final long SHORT_REFRESH_TOKEN_EXPIRY = 1000L * 60L * 60L * 24L * 7L; // 7 days
    private static final long LONG_REFRESH_TOKEN_EXPIRY = 1000L * 60L * 60L * 24L * 30L; // 30 days

    private SecretKey key;

    @Value("${secreteJwtString}")
    private String secreteJwtString;

    @PostConstruct
    public void init(){
        byte[] keyBytes = secreteJwtString.getBytes(StandardCharsets.UTF_8);
        this.key = new SecretKeySpec(keyBytes, "HmacSHA256");
    }

    // ✅ FIXED: Generate tokens with userId
    public String generateAccessToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId().toString());
        claims.put("email", user.getEmail());
        claims.put("name", user.getName());
        claims.put("role", user.getUserRole());
        claims.put("type", "access");

        log.info("🔑 Generating access token for user ID: {}", user.getId());

        return Jwts.builder()
                .claims(claims)
                .subject(user.getEmail())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + ACCESS_TOKEN_EXPIRY))
                .signWith(key)
                .compact();
    }

    // ✅ FIXED: Generate refresh token with userId
    public String generateRefreshToken(User user, boolean rememberMe) {
        long expiry = rememberMe ? LONG_REFRESH_TOKEN_EXPIRY : SHORT_REFRESH_TOKEN_EXPIRY;

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId().toString());
        claims.put("email", user.getEmail());
        claims.put("type", "refresh");
        claims.put("rememberMe", rememberMe);

        log.info("🔑 Generating refresh token for user ID: {}, rememberMe: {}", user.getId(), rememberMe);

        return Jwts.builder()
                .claims(claims)
                .subject(user.getEmail())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiry))
                .signWith(key)
                .compact();
    }

    public Boolean isRememberMeToken(String token) {
        return extractClaims(token, claims -> claims.get("rememberMe", Boolean.class));
    }

    public String getUsernameFromToken(String token){
        return extractClaims(token, Claims::getSubject);
    }

    private <T> T extractClaims(String token, Function<Claims, T> claimsTFunction){
        return claimsTFunction.apply(Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload());
    }

    public boolean isTokenValid(String token, UserDetails userDetails){
        final String username = getUsernameFromToken(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    public boolean isTokenValid(String token) {
        try {
            return !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isTokenExpired(String token){
        return extractClaims(token, Claims::getExpiration).before(new Date());
    }

    public String extractJwtFromRequest(HttpServletRequest request) {
        final String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    // ✅ FIXED: Extract User ID from token
    public UUID getUserIdFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String userId = claims.get("userId", String.class);
            if (userId == null || userId.isEmpty()) return null;
            log.info("🔍 Extracted userId from token: {}", userId);
            return UUID.fromString(userId);
        } catch (Exception e) {
            log.error("❌ Error extracting userId from token: {}", e.getMessage());
            return null;
        }
    }

    // ✅ NEW: Extract user role from token
    public String getUserRoleFromToken(String token) {
        return extractClaims(token, claims -> claims.get("role", String.class));
    }

    // ✅ NEW: Extract email from token
    public String getUserEmailFromToken(String token) {
        return extractClaims(token, claims -> claims.get("email", String.class));
    }
}