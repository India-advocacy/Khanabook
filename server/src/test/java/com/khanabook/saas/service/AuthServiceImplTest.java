package com.khanabook.saas.service;

import com.khanabook.saas.controller.AuthController.*;
import com.khanabook.saas.entity.RestaurantProfile;
import com.khanabook.saas.entity.User;
import com.khanabook.saas.entity.UserRole;
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

    @Test
    void login_success() {
        User user = activeUser("+919876543210", "hashed", 100L);
        when(userRepository.findByEmail("+919876543210")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("pass123", "hashed")).thenReturn(true);
        when(jwtUtility.generateToken(anyString(), anyLong(), anyString())).thenReturn("jwt-token");

        AuthResponse resp = authService.login(loginRequest("+919876543210", "pass123"));

        assertThat(resp.getToken()).isEqualTo("jwt-token");
        assertThat(resp.getRestaurantId()).isEqualTo(100L);
    }

    @Test
    void signup_newUser_createsProfileAndUser() {
        when(userRepository.findByEmail("+919876543210")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("pass123")).thenReturn("bcrypt-hash");
        when(jwtUtility.generateToken(anyString(), anyLong(), anyString())).thenReturn("signup-token");

        SignupRequest req = new SignupRequest("+919876543210", "Nandha", "pass123", "DEVICE_A");
        AuthResponse resp = authService.signup(req);

        assertThat(resp.getToken()).isEqualTo("signup-token");
        assertThat(resp.getUserName()).isEqualTo("Nandha");

        ArgumentCaptor<RestaurantProfile> profileCaptor = ArgumentCaptor.forClass(RestaurantProfile.class);
        verify(restaurantProfileRepository).save(profileCaptor.capture());
        assertThat(profileCaptor.getValue().getShopName()).contains("Nandha");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getPasswordHash()).isEqualTo("bcrypt-hash");
        assertThat(savedUser.getIsActive()).isTrue();
    }

    @Test
    void signup_restaurantIdIsUuidBased_notSequential() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("hash");
        when(jwtUtility.generateToken(anyString(), anyLong(), anyString())).thenReturn("t");

        AuthResponse r1 = authService.signup(new SignupRequest("+911111111111", "A", "p", "D1"));
        AuthResponse r2 = authService.signup(new SignupRequest("+912222222222", "B", "p", "D2"));

        assertThat(r1.getRestaurantId()).isNotEqualTo(r2.getRestaurantId());
    }

    private User activeUser(String phone, String hash, Long restaurantId) {
        User u = new User();
        u.setEmail(phone);
        u.setPasswordHash(hash);
        u.setRestaurantId(restaurantId);
        u.setRole(UserRole.OWNER);
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
