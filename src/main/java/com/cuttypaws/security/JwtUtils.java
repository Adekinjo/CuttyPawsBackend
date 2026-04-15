package com.cuttypaws.security;

import com.cuttypaws.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
@Slf4j
public class JwtUtils {

    private static final long ACCESS_TOKEN_EXPIRY = 1000L * 60L * 15L;          // 15 minutes
    private static final long SHORT_REFRESH_TOKEN_EXPIRY = 1000L * 60L * 60L * 24L * 7L;   // 7 days
    private static final long LONG_REFRESH_TOKEN_EXPIRY = 1000L * 60L * 60L * 24L * 30L;   // 30 days

    private static final String CLAIM_USER_ID = "userId";
    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_NAME = "name";
    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_TYPE = "type";
    private static final String CLAIM_REMEMBER_ME = "rememberMe";

    private static final String TOKEN_TYPE_ACCESS = "access";
    private static final String TOKEN_TYPE_REFRESH = "refresh";

    private SecretKey key;


    @Value("${secreteJwtString}")
    private String jwtSecret;

    @PostConstruct
    public void init() {
        if (!StringUtils.hasText(jwtSecret)) {
            throw new IllegalStateException("JWT secret is missing. Set jwt.secret or secreteJwtString.");
        }

        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("JWT secret must be at least 32 bytes long for HS256.");
        }

        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateAccessToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_USER_ID, user.getId().toString());
        claims.put(CLAIM_EMAIL, user.getEmail());
        claims.put(CLAIM_NAME, user.getName());
        claims.put(CLAIM_ROLE, user.getUserRole().name());
        claims.put(CLAIM_TYPE, TOKEN_TYPE_ACCESS);

        Date now = new Date();
        Date expiry = new Date(now.getTime() + ACCESS_TOKEN_EXPIRY);

        return Jwts.builder()
                .claims(claims)
                .subject(user.getEmail())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    public String generateRefreshToken(User user, boolean rememberMe) {
        long expiryMs = rememberMe ? LONG_REFRESH_TOKEN_EXPIRY : SHORT_REFRESH_TOKEN_EXPIRY;

        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_USER_ID, user.getId().toString());
        claims.put(CLAIM_EMAIL, user.getEmail());
        claims.put(CLAIM_TYPE, TOKEN_TYPE_REFRESH);
        claims.put(CLAIM_REMEMBER_ME, rememberMe);

        Date now = new Date();
        Date expiry = new Date(now.getTime() + expiryMs);

        return Jwts.builder()
                .claims(claims)
                .subject(user.getEmail())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    public Date getExpirationFromToken(String token) {
        return extractClaims(token, Claims::getExpiration);
    }

    public LocalDateTime getExpirationAsLocalDateTime(String token) {
        return getExpirationFromToken(token)
                .toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
    }

    public String extractJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    public String getUsernameFromToken(String token) {
        return extractClaims(token, Claims::getSubject);
    }

    public UUID getUserIdFromToken(String token) {
        String userId = extractClaims(token, claims -> claims.get(CLAIM_USER_ID, String.class));
        if (!StringUtils.hasText(userId)) {
            return null;
        }
        return UUID.fromString(userId);
    }

    public String getTokenType(String token) {
        return extractClaims(token, claims -> claims.get(CLAIM_TYPE, String.class));
    }

    public boolean isAccessToken(String token) {
        return TOKEN_TYPE_ACCESS.equals(getTokenType(token));
    }

    public boolean isRefreshToken(String token) {
        return TOKEN_TYPE_REFRESH.equals(getTokenType(token));
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            String username = getUsernameFromToken(token);
            return StringUtils.hasText(username)
                    && username.equals(userDetails.getUsername())
                    && !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public boolean isAccessTokenValid(String token, UserDetails userDetails) {
        return isAccessToken(token) && isTokenValid(token, userDetails);
    }

    public boolean isTokenValid(String token) {
        try {
            return !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private boolean isTokenExpired(String token) {
        Date expiration = extractClaims(token, Claims::getExpiration);
        return expiration.before(new Date());
    }

    private <T> T extractClaims(String token, Function<Claims, T> claimsResolver) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claimsResolver.apply(claims);
    }
}