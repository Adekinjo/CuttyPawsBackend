

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

        String uri = request.getRequestURI();
        String method = request.getMethod();

        if (uri.startsWith("/auth/") ||
                uri.equals("/post/get-all") ||
                uri.equals("/ai/chat") ||
                uri.equals("/ai/search/parse") ||
                uri.equals("/ai/pet-health") ||
                uri.equals("/ai/pet-health/image") ||
                uri.startsWith("/post/get-all/") ||
                uri.startsWith("/comments/") ||
                uri.startsWith("/products/") ||
                uri.startsWith("/product/suggestions") ||
                uri.startsWith("/product/search") ||
                uri.startsWith("/category/get-all") ||
                uri.startsWith("/search") ||
                uri.startsWith("/ws/") ||
                uri.startsWith("/topic/") ||
                uri.startsWith("/queue/") ||
                uri.equals("/favicon.ico") ||
                uri.startsWith("/error") ||
                uri.startsWith("/webhook/stripe") ||
                uri.startsWith("/services/public/") ||
                ("GET".equalsIgnoreCase(method) && uri.startsWith("/service-reviews/")) ||
                uri.startsWith("/products/filter-by-name-and-category")) {
            filterChain.doFilter(request, response);
            return;
        }

        if ("OPTIONS".equalsIgnoreCase(method)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = getTokenFromRequest(request);
        if (token != null) {
            try {
                String username = jwtUtils.getUsernameFromToken(token);
                UserDetails userDetails = customUserDetailsService.loadUserByUsername(username);

                if (StringUtils.hasText(username) && jwtUtils.isTokenValid(token, userDetails)) {
                    UsernamePasswordAuthenticationToken authenticationToken =
                            new UsernamePasswordAuthenticationToken(
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
