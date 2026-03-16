package com.khanabook.saas.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtRequestFilter jwtRequestFilter;

    /**
     * BCrypt encoder shared across the application (AuthService uses this for
     * hashing; the bean is here to avoid circular dependency).
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            // Stateless — no HTTP sessions
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Only auth endpoints (login, signup, check-user, google login) are public.
                // NOTE: paths are relative to context-path (/api/v1). 
                // Public: /api/v1/auth/login, /api/v1/auth/signup, etc.
                .requestMatchers("/auth/login", "/auth/signup", "/auth/google", "/auth/check-user", "/auth/reset-password").permitAll()

                // password reset now requires authentication for security (Change Password flow)
                // TODO: For "Forgot Password" (unauthenticated), implement OTP-gated logic.
                .anyRequest().authenticated()
            )
            // Run our JWT filter before Spring's username/password filter
            .addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
