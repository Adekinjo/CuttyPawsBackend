//
//package com.cuttypaws.security;
//
//import lombok.RequiredArgsConstructor;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.security.authentication.AuthenticationManager;
//import org.springframework.security.config.Customizer;
//import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
//import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
//import org.springframework.security.config.annotation.web.builders.HttpSecurity;
//import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
//import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
//import org.springframework.security.config.http.SessionCreationPolicy;
//import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.security.web.SecurityFilterChain;
//import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
//
//@Configuration
//@EnableWebSecurity
//@EnableMethodSecurity
//@RequiredArgsConstructor
//public class SecurityConfig {
//
//    private final JwtAuthFilter jwtAuthFilter;
//    private final SecurityFilter securityFilter;
//    private final SecurityHeadersFilter securityHeadersFilter;
//
//    @Bean
//    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
//        httpSecurity
//                .csrf(AbstractHttpConfigurer::disable)
//                .cors(Customizer.withDefaults())
//                .authorizeHttpRequests(request -> request
//                        .requestMatchers("/admin/**").hasAuthority("ROLE_ADMIN")
//                        .requestMatchers("/customer-support/**").hasAnyAuthority("ROLE_CUSTOMER_SUPPORT","ROLE_ADMIN")
//                        .requestMatchers("/order/**").hasAnyAuthority("ROLE_ADMIN","ROLE_USER", "ROLE_CUSTOMER_SUPPORT", "ROLE_COMPANY")
//                        .requestMatchers("/company/**").hasAnyAuthority("ROLE_COMPANY","ROLE_ADMIN", "ROLE_CUSTOMER_SUPPORT")
//                        .requestMatchers("/auth/**","/order/**","/deals/**", "/reviews/**", "/category/**", "/sub-category/**", "/product/**", "/search/**", "/newsletter/**").permitAll()
//                        .requestMatchers("/payment/**","/order/create").authenticated()
//                        .anyRequest().authenticated()
//                )
//                .sessionManagement(manager -> manager
//                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
//                )
//                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
//                .addFilterBefore(securityFilter, UsernamePasswordAuthenticationFilter.class)
//                .addFilterBefore(securityHeadersFilter, UsernamePasswordAuthenticationFilter.class);
//
//        return httpSecurity.build();
//    }
//
//    @Bean
//    public PasswordEncoder passwordEncoder() {
//        return new BCryptPasswordEncoder(12);
//    }
//
//    @Bean
//    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
//        return authenticationConfiguration.getAuthenticationManager();
//    }
//}






package com.cuttypaws.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
                .authorizeHttpRequests(req -> req
                        // WEBSOCKET
                        .requestMatchers("/ws/**", "/topic/**", "/queue/**", "/user/**").permitAll()

                        // PUBLIC AUTH & PUBLIC CONTENT
                        .requestMatchers(
                                "/auth/**",
                                "/post/all",
                                "/post/get-all",
                                "/likes/**",
                                "/comments/**",
                                "/deals/**",
                                "/reviews/**",
                                "/category/**",
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
                                "/users/update"
                        ).authenticated()

                        // E-COMMERCE PROTECTED ENDPOINTS
                        .requestMatchers("/payment/**", "/order/create").authenticated()

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
                .addFilterBefore(securityFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(securityHeadersFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(
                "https://www.kinjomarket.com",
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