

package com.cuttypaws.security;

import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final CustomUserDetailsService customUserDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // Skip JWT validation for public endpoints
        if (request.getRequestURI().startsWith("/auth/") ||
                request.getRequestURI().equals("/post/get-all") ||
                request.getRequestURI().startsWith("/post/get-all/") ||
                request.getRequestURI().startsWith("/comments/") ||
                request.getRequestURI().startsWith("/products/") ||
                request.getRequestURI().startsWith("/product/suggestions") ||
                request.getRequestURI().startsWith("/product/search") ||
                request.getRequestURI().startsWith("/category/get-all") ||
                request.getRequestURI().startsWith("/search") ||
                request.getRequestURI().startsWith("/ws/") ||
                request.getRequestURI().startsWith("/topic/") ||
                request.getRequestURI().startsWith("/queue/") ||
                request.getRequestURI().equals("/favicon.ico") ||
                request.getRequestURI().startsWith("/error") ||

        request.getRequestURI().startsWith("/products/filter-by-name-and-category")) {
            filterChain.doFilter(request, response);
            return;
        }
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        // Extract and validate JWT token for other endpoints
        String token = getTokenFromRequest(request);
        if (token != null) {
            try {
                String username = jwtUtils.getUsernameFromToken(token);
                UserDetails userDetails = customUserDetailsService.loadUserByUsername(username);

                if (StringUtils.hasText(username) && jwtUtils.isTokenValid(token, userDetails)) {
                    log.info("VALID JWT FOR {}", username);

                    UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities()
                    );
                    authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                }
            } catch (ExpiredJwtException e) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"status\":401,\"message\":\"Token expired. Please log in again.\"}");
                return;
            } catch (Exception e) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"status\":401,\"message\":\"Invalid token. Please log in again.\"}");
                return;
            }

        }

        filterChain.doFilter(request, response);
    }

    private String getTokenFromRequest(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (StringUtils.hasText(token) && StringUtils.startsWithIgnoreCase(token, "Bearer ")) {
            return token.substring(7);
        }
        return null;
    }
}
