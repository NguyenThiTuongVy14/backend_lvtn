package com.example.test.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import javax.crypto.SecretKey;
import io.jsonwebtoken.security.Keys;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final UserDetailsService userDetailsService;
    private final JwtRequestFilter jwtRequestFilter;
    @Value("${jwt.secret}")
    private String jwtSecret;

    public SecurityConfig(UserDetailsService userDetailsService, @Lazy JwtRequestFilter jwtRequestFilter) {
        this.userDetailsService = userDetailsService;
        this.jwtRequestFilter = jwtRequestFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public SecretKey secretKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints
                        .requestMatchers("/api/auth/login", "/api/auth/logout").permitAll()
                        .requestMatchers("/ws/**").permitAll()

                        // Collection Points endpoints
                        .requestMatchers("/api/collection-points/mark-completed")
                        .hasAnyAuthority("COLLECTOR", "ADMIN")
                        .requestMatchers("/api/collection-points/*/complete-driver-collection")
                        .hasAnyAuthority("DRIVER", "ADMIN")
                        .requestMatchers("/api/collection-points/driver-assigned")
                        .hasAnyAuthority("DRIVER", "ADMIN")
                        .requestMatchers("/api/collection-points/nearest-pending")
                        .hasAnyAuthority("DRIVER", "ADMIN")
                        .requestMatchers("/api/collection-points/**")
                        .hasAnyAuthority("COLLECTOR", "DRIVER", "ADMIN")

                        // Job Positions endpoints
                        .requestMatchers(HttpMethod.POST, "/api/job-positions")
                        .hasAuthority("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/job-positions")
                        .hasAnyAuthority("ADMIN","DRIVER","COLLECTOR")
                        .requestMatchers(HttpMethod.PUT, "/api/job-positions/**")
                        .hasAuthority("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/job-positions/**")
                        .hasAuthority("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/job-positions/*")
                        .hasAnyAuthority("COLLECTOR", "DRIVER", "ADMIN")
                        .requestMatchers("/api/job-positions/nearby")
                        .hasAnyAuthority("COLLECTOR", "DRIVER", "ADMIN")

                        // Job Rotations endpoints
                        .requestMatchers(HttpMethod.POST, "/api/job-rotations")
                        .hasAuthority("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/job-rotations")
                        .hasAuthority("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/job-rotations/**")
                        .hasAuthority("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/job-rotations/**")
                        .hasAuthority("ADMIN")
                        .requestMatchers("/api/job-rotations/staff/**")
                        .hasAuthority("ADMIN")
                        .requestMatchers("/api/job-rotations/status/**")
                        .hasAuthority("ADMIN")
                        .requestMatchers("/api/job-rotations/auto-assign")
                        .hasAuthority("ADMIN")
                        .requestMatchers("/api/job-rotations/statistics")
                        .hasAuthority("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/job-rotations/*")
                        .hasAnyAuthority("COLLECTOR", "DRIVER", "ADMIN")
                        .requestMatchers("/api/job-rotations/me")
                        .hasAnyAuthority("COLLECTOR", "DRIVER", "ADMIN")
                        .requestMatchers("/api/job-rotations/my-active-rotations")
                        .hasAnyAuthority("COLLECTOR", "DRIVER", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/job-rotations/collector/**")
                        .hasAuthority("COLLECTOR")
                        .requestMatchers(HttpMethod.POST, "/api/job-rotations/driver/**")
                        .hasAuthority("DRIVER")
                        .requestMatchers(HttpMethod.GET, "/api/job-rotations/driver/**")
                        .hasAuthority("DRIVER")

                        // Staff endpoints
                        .requestMatchers(HttpMethod.POST, "/api/staff")
                        .hasAuthority("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/staff")
                        .hasAuthority("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/staff/*")
                        .hasAuthority("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/staff/*")
                        .hasAuthority("ADMIN")
                        .requestMatchers("/api/staff/role/**")
                        .hasAuthority("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/staff/*")
                        .hasAnyAuthority("COLLECTOR", "DRIVER", "ADMIN")
                        .requestMatchers("/api/staff/profile")
                        .hasAnyAuthority("COLLECTOR", "DRIVER", "ADMIN")

                        .requestMatchers(HttpMethod.GET,"/api/test-auth")
                        .hasAnyAuthority("COLLECTOR", "DRIVER", "ADMIN")

                        .requestMatchers(HttpMethod.GET,"/api/shifts")
                        .hasAnyAuthority("COLLECTOR", "DRIVER", "ADMIN")
                        //fcm
                        .requestMatchers(HttpMethod.GET,"/api/notifications/**")
                        .hasAnyAuthority("COLLECTOR", "DRIVER", "ADMIN")
                        .requestMatchers(HttpMethod.POST,"/api/notifications/**")
                        .hasAnyAuthority("COLLECTOR", "DRIVER", "ADMIN")
                        // Tất cả endpoints khác - chỉ ADMIN
                        .anyRequest().hasAuthority("ADMIN")
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(exception -> exception
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpStatus.FORBIDDEN.value());
                            response.setContentType("application/json");
                            response.getWriter().write("{\"error\": \"Access denied: Insufficient permissions\"}");
                        })
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpStatus.UNAUTHORIZED.value());
                            response.setContentType("application/json");
                            response.getWriter().write("{\"error\": \"Unauthorized: Invalid or missing token\"}");
                        })
                );

        return http.build();
    }
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("*")); // Cho phép tất cả nguồn gốc (hoặc chỉ định IP/cổng cụ thể)
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}