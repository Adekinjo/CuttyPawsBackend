package com.cuttypaws.security;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

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
                .cors(cors -> cors.configurationSource(corsConfigurationSource())) // ✅ Use custom CORS config
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
                        }))


                .authorizeHttpRequests(req -> req
                        // WEBSOCKET
                        .requestMatchers("/ws/**", "/topic/**", "/queue/**").permitAll()

                        // PUBLIC AUTH & PUBLIC CONTENT
                        .requestMatchers(
                                "/auth/**",
                                "/post/get-all",
                                "/likes/**",
                                "/comments/**",
                                "/deals/**",
                                "/reviews/**",
                                "/category/get-all",
                                "/sub-category/**",
                                "/product/**",
                                "/search/**",
                                "/newsletter/**"
                        ).permitAll()

                        // SOCIAL PROTECTED ENDPOINTS
                        .requestMatchers(
                                "/post/create",
                                "/post/my-posts",
                                "/follow/**",
                                "/likes/{postId}/react",
                                "/comments/create/",
                                "/notifications/**",
                                "/pet/**",
                                "/users/my-info",
                                "/users/update-profile-image",
                                "/users/update-cover-image",
                                "/users/update",
                                "/orders/**"
                        ).authenticated()

                        // E-COMMERCE PROTECTED ENDPOINTS
                        .requestMatchers("/payment/**", "/order/create").authenticated()
                        .requestMatchers("/auth/update-user-profile").authenticated()

                        // ROLE-BASED ACCESS
                        .requestMatchers("/admin/**").hasAuthority("ROLE_ADMIN")
                        .requestMatchers("/customer-support/**")
                        .hasAnyAuthority("ROLE_CUSTOMER_SUPPORT", "ROLE_ADMIN")
                        .requestMatchers("/company/**")
                        .hasAnyAuthority("ROLE_COMPANY", "ROLE_ADMIN", "ROLE_CUSTOMER_SUPPORT")
                        .requestMatchers("/order/**")
                        .hasAnyAuthority("ROLE_ADMIN", "ROLE_USER", "ROLE_CUSTOMER_SUPPORT", "ROLE_COMPANY")

                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(securityFilter, JwtAuthFilter.class)
                .addFilterAfter(securityHeadersFilter, SecurityFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(
                "https://www.cuttypaws.com",
                "http://localhost:9494",
                "http://localhost:5173"
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setExposedHeaders(Arrays.asList("Authorization", "Content-Type"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config
    ) throws Exception {
        return config.getAuthenticationManager();
    }
}