package com.khanabook.saas.service;

import com.khanabook.saas.controller.AuthController.*;
import com.khanabook.saas.entity.RestaurantProfile;
import com.khanabook.saas.entity.User;
import com.khanabook.saas.repository.RestaurantProfileRepository;
import com.khanabook.saas.repository.UserRepository;
import com.khanabook.saas.service.impl.AuthServiceImpl;
import com.khanabook.saas.utility.JwtUtility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private RestaurantProfileRepository restaurantProfileRepository;
    @Mock private JwtUtility jwtUtility;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthServiceImpl authService;

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(authService, "googleClientId", "test-google-client-id");
    }

    // ─── validateConfig ───────────────────────────────────────────────────────

    @Test
    void validateConfig_whenClientIdBlank_throwsIllegalState() {
        ReflectionTestUtils.setField(authService, "googleClientId", "");
        assertThatThrownBy(() -> authService.validateConfig())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("google.client.id");
    }

    @Test
    void validateConfig_whenClientIdSet_doesNotThrow() {
        assertThatNoException().isThrownBy(() -> authService.validateConfig());
    }

    // ─── login ────────────────────────────────────────────────────────────────

    @Test
    void login_success() {
        User user = activeUser("+919876543210", "hashed", 100L);
        when(userRepository.findByEmail("+919876543210")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("pass123", "hashed")).thenReturn(true);
        when(jwtUtility.generateToken(anyString(), anyLong())).thenReturn("jwt-token");

        AuthResponse resp = authService.login(loginRequest("+919876543210", "pass123"));

        assertThat(resp.getToken()).isEqualTo("jwt-token");
        assertThat(resp.getRestaurantId()).isEqualTo(100L);
    }

    @Test
    void login_wrongPassword_throwsIllegalArgument() {
        User user = activeUser("+919876543210", "hashed", 100L);
        when(userRepository.findByEmail("+919876543210")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(loginRequest("+919876543210", "wrong")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid phone number or password");
    }

    @Test
    void login_userNotFound_throwsIllegalArgument() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(loginRequest("+919999999999", "pass")))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void login_inactiveUser_throwsIllegalArgument() {
        User user = activeUser("+919876543210", "hashed", 100L);
        user.setIsActive(false);
        when(userRepository.findByEmail("+919876543210")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

        assertThatThrownBy(() -> authService.login(loginRequest("+919876543210", "pass")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("disabled");
    }

    // ─── signup ───────────────────────────────────────────────────────────────

    @Test
    void signup_newUser_createsProfileAndUser() {
        when(userRepository.findByEmail("+919876543210")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("pass123")).thenReturn("bcrypt-hash");
        when(jwtUtility.generateToken(anyString(), anyLong())).thenReturn("signup-token");

        SignupRequest req = new SignupRequest("+919876543210", "Nandha", "pass123", "DEVICE_A");
        AuthResponse resp = authService.signup(req);

        assertThat(resp.getToken()).isEqualTo("signup-token");
        assertThat(resp.getUserName()).isEqualTo("Nandha");

        // Profile must be saved
        ArgumentCaptor<RestaurantProfile> profileCaptor = ArgumentCaptor.forClass(RestaurantProfile.class);
        verify(restaurantProfileRepository).save(profileCaptor.capture());
        assertThat(profileCaptor.getValue().getShopName()).contains("Nandha");

        // User must be saved with hashed password, never plain
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getPasswordHash()).isEqualTo("bcrypt-hash");
        assertThat(savedUser.getPasswordHash()).doesNotContain("pass123");
        assertThat(savedUser.getIsActive()).isTrue();
        assertThat(savedUser.getCreatedAt()).isNotNull();
    }

    @Test
    void signup_existingPhoneNumber_throwsIllegalArgument() {
        when(userRepository.findByEmail("+919876543210")).thenReturn(Optional.of(new User()));

        assertThatThrownBy(() -> authService.signup(new SignupRequest("+919876543210", "A", "pass123", "D")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("already exists");
    }

    @Test
    void signup_restaurantIdIsUuidBased_notSequential() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("hash");
        when(jwtUtility.generateToken(anyString(), anyLong())).thenReturn("t");

        AuthResponse r1 = authService.signup(new SignupRequest("+911111111111", "A", "p", "D1"));
        AuthResponse r2 = authService.signup(new SignupRequest("+912222222222", "B", "p", "D2"));

        // IDs should be different and large (UUID-based, not sequential 1, 2, 3)
        assertThat(r1.getRestaurantId()).isNotEqualTo(r2.getRestaurantId());
        assertThat(r1.getRestaurantId()).isGreaterThan(1000L);
    }

    // ─── resetPassword ────────────────────────────────────────────────────────

    @Test
    void resetPassword_updatesHashAndTimestamp() {
        User user = activeUser("+919876543210", "old-hash", 100L);
        when(userRepository.findByEmail("+919876543210")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newpass")).thenReturn("new-hash");

        authService.resetPassword("+919876543210", "newpass");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("new-hash");
        assertThat(captor.getValue().getUpdatedAt()).isPositive();
    }

    @Test
    void resetPassword_userNotFound_throwsIllegalArgument() {
        when(userRepository.findByEmail("+910000000000")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> authService.resetPassword("+910000000000", "x"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    // ─── checkUserExists ─────────────────────────────────────────────────────

    @Test
    void checkUserExists_returnsTrue_whenFound() {
        when(userRepository.existsByEmail("+919876543210")).thenReturn(true);
        assertThat(authService.checkUserExists("+919876543210")).isTrue();
    }

    @Test
    void checkUserExists_returnsFalse_whenNotFound() {
        when(userRepository.existsByEmail("+910000000000")).thenReturn(false);
        assertThat(authService.checkUserExists("+910000000000")).isFalse();
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private User activeUser(String phone, String hash, Long restaurantId) {
        User u = new User();
        u.setEmail(phone);
        u.setPasswordHash(hash);
        u.setRestaurantId(restaurantId);
        u.setName("Test User");
        u.setIsActive(true);
        return u;
    }

    private LoginRequest loginRequest(String phone, String password) {
        LoginRequest r = new LoginRequest();
        r.setPhoneNumber(phone);
        r.setPassword(password);
        return r;
    }
}
