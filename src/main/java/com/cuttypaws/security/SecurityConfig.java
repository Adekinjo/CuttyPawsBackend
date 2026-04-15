package com.cuttypaws.security;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final SecurityFilter securityFilter;
    private final SecurityHeadersFilter securityHeadersFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"status\":401,\"message\":\"Please log in to continue\"}");
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"status\":403,\"message\":\"You do not have permission to access this resource\"}");
                        })
                )
                .authorizeHttpRequests(req -> req

                        // Preflight
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Public websocket handshake/topics if intentionally public
                        .requestMatchers("/ws/**", "/topic/**", "/queue/**").permitAll()

                        // ---- PUBLIC AUTH ENDPOINTS ONLY ----
                        .requestMatchers(
                                "/auth/login",
                                "/auth/register",
                                "/auth/refresh-token",
                                "/auth/verify-code",
                                "/auth/resend-verification",
                                "/auth/request-password-reset",
                                "/auth/reset-password"
                        ).permitAll()

                        // ---- PUBLIC WEBHOOKS ----
                        .requestMatchers("/webhook/stripe").permitAll()

                        // ---- PUBLIC READ ENDPOINTS ----
                        .requestMatchers(HttpMethod.GET,
                                "/post/get-all",
                                "/post/get-all/**",
                                "/comments/**",
                                "/deals/**",
                                "/reviews/**",
                                "/category/get-all",
                                "/sub-category/**",
                                "/feed/**",
                                "/product/**",
                                "/products/**",
                                "/product/suggestions/**",
                                "/product/search/**",
                                "/products/filter-by-name-and-category/**",
                                "/search/**",
                                "/newsletter/**",
                                "/services/public/**",
                                "/service-reviews/**",
                                "/ai/**"
                        ).permitAll()

                        // ---- AUTHENTICATED USER ENDPOINTS ----
                        .requestMatchers(
                                "/auth/update-user-profile",
                                "/post/create",
                                "/post/my-posts",
                                "/follow/**",
                                "/likes/*/react",
                                "/comments/create",
                                "/notifications/**",
                                "/pet/**",
                                "/user/my-info",
                                "/user/update-profile-image",
                                "/user/update-cover-image",
                                "/user/update",
                                "/users/login-history",
                                "/order/create",
                                "/payment/**",
                                "/service-booking-reports/**",
                                "/services/my-profile",
                                "/services/my-dashboard",
                                "/service-ads/**"
                        ).authenticated()

                        .requestMatchers(HttpMethod.POST, "/service-reviews/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/order/**").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/order/**").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/order/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/order/**").authenticated()

                        // ---- ROLE-BASED ENDPOINTS ----
                        .requestMatchers("/services/admin/**").hasAuthority("ROLE_ADMIN")
                        .requestMatchers("/admin/**").hasAuthority("ROLE_ADMIN")
                        .requestMatchers("/customer-support/**")
                        .hasAnyAuthority("ROLE_CUSTOMER_SUPPORT", "ROLE_ADMIN")
                        .requestMatchers("/company/**")
                        .hasAnyAuthority("ROLE_SELLER", "ROLE_ADMIN", "ROLE_CUSTOMER_SUPPORT")

                        .anyRequest().authenticated()
                )

                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(securityFilter, JwtAuthFilter.class)
                .addFilterAfter(securityHeadersFilter, SecurityFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}