package com.khanabook.saas.controller;

import com.khanabook.saas.service.AuthService;
import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Auth controller — thin layer that delegates entirely to AuthService.
 *
 * CRIT-01: Routes here (/auth/**) are the ONLY publicly accessible endpoints.
 *           All other routes require a valid JWT (enforced in SecurityConfig).
 * CRIT-03: Password is hashed by AuthService using BCrypt — never stored plain.
 * CRIT-04: Google OAuth mock has been removed. Implement real Google ID token
 *           verification on the server using google-auth-library-oauth2-http
 *           before enabling Google login in production.
 * ARCH-01: Zero business logic or repository access here.
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(@Valid @RequestBody SignupRequest request) {
        return ResponseEntity.ok(authService.signup(request));
    }

    /**
     * Google login
     */
    @PostMapping("/google")
    public ResponseEntity<AuthResponse> loginWithGoogle(@Valid @RequestBody GoogleLoginRequest request) {
        return ResponseEntity.ok(authService.googleLogin(request));
    }

    @GetMapping("/check-user")
    public ResponseEntity<Boolean> checkUser(@RequestParam String phoneNumber) {
        return ResponseEntity.ok(authService.checkUserExists(phoneNumber));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@RequestParam String phoneNumber, @RequestParam String newPassword) {
        authService.resetPassword(phoneNumber, newPassword);
        return ResponseEntity.ok().build();
    }

    // ─── Request / Response DTOs ──────────────────────────────────────────────

    @Data @AllArgsConstructor @NoArgsConstructor
    public static class LoginRequest {
        @NotBlank(message = "Phone number is required")
        @Pattern(regexp = "^\\+?[1-9]\\d{6,19}$", message = "Phone number must be valid format")
        @Size(max = 15)
        @JsonAlias("email")
        private String phoneNumber;

        /** The client sends the plain password; server verifies against BCrypt hash. */
        @NotBlank(message = "Password is required")
        @Size(min = 6, max = 128, message = "Password must be between 6 and 128 characters")
        private String password;

        @Size(max = 128)
        private String deviceId;
    }

    @Data @AllArgsConstructor @NoArgsConstructor
    public static class GoogleLoginRequest {
        @NotBlank(message = "idToken is required")
        private String idToken;
        private String deviceId;
    }

    @Data @AllArgsConstructor @NoArgsConstructor
    public static class SignupRequest {
        @NotBlank(message = "Phone number is required")
        @Pattern(regexp = "^\\+?[1-9]\\d{6,19}$", message = "Phone number must be valid format")
        @Size(max = 15)
        @JsonAlias("email")
        private String phoneNumber;

        @NotBlank(message = "Name is required")
        @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
        private String name;

        @NotBlank(message = "Password is required")
        @Size(min = 6, max = 128, message = "Password must be between 6 and 128 characters")
        private String password;

        @Size(max = 128)
        private String deviceId;
    }

    @Data @AllArgsConstructor @NoArgsConstructor
    public static class AuthResponse {
        private String token;
        private Long restaurantId;
        private String userName;
        private String loginId;
        private String whatsappNumber;
    }
}
