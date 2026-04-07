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
                .cors(Customizer.withDefaults()) // ✅ uses CorsConfigurationSource from CorsConfig
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
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

                        // allow preflight
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // websocket
                        .requestMatchers("/ws/**", "/topic/**", "/queue/**").permitAll()

                        // public endpoints
                        .requestMatchers(
                                "/auth/**",
                                "/post/get-all",
                                "/ai/**",
                                "/webhook/stripe",
                                "/post/get-all/**",
                                "/likes/**",
                                "/comments/**",
                                "/deals/**",
                                "/reviews/**",
                                "/category/get-all",
                                "/sub-category/**",
                                "/feed/**",
                                "/product/**",
                                "/search/**",
                                "/newsletter/**",
                                "/services/public/**"
                        ).permitAll()

                        // public GET service reviews
                        .requestMatchers(HttpMethod.GET, "/service-reviews/**").permitAll()

                        // admin service moderation endpoints
                        .requestMatchers("/services/admin/**").hasAuthority("ROLE_ADMIN")

                        // authenticated service provider endpoints
                        .requestMatchers(
                                "/services/my-profile",
                                "/services/my-dashboard",
                                "/service-ads/**"
                        ).authenticated()

                        // authenticated review creation/update
                        .requestMatchers(HttpMethod.POST, "/service-reviews/**").authenticated()

                        // authenticated general user endpoints
                        .requestMatchers(
                                "/post/create",
                                "/post/my-posts",
                                "/follow/**",
                                "/likes/{postId}/react",
                                "/comments/create/",
                                "/notifications/**",
                                "/pet/**",
                                "/user/my-info",
                                "/user/update-profile-image",
                                "/user/update-cover-image",
                                "/user/update",
                                "/order/**",
                                "/payment/**",
                                "/service-booking-reports/**",
                                "/order/create",
                                "/auth/update-user-profile"
                        ).authenticated()

                        // role based
                        .requestMatchers("/admin/**").hasAuthority("ROLE_ADMIN")
                        .requestMatchers("/customer-support/**")
                        .hasAnyAuthority("ROLE_CUSTOMER_SUPPORT", "ROLE_ADMIN")
                        .requestMatchers("/company/**")
                        .hasAnyAuthority("ROLE_SELLER", "ROLE_ADMIN", "ROLE_CUSTOMER_SUPPORT")
                        .requestMatchers("/order/**")
                        .hasAnyAuthority("ROLE_ADMIN", "ROLE_USER", "ROLE_CUSTOMER_SUPPORT", "ROLE_SELLER")

                        .anyRequest().authenticated()
                )

                // Filters
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