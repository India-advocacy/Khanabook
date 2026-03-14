package com.khanabook.saas.service.impl;

import com.khanabook.saas.controller.AuthController.AuthResponse;
import com.khanabook.saas.controller.AuthController.LoginRequest;
import com.khanabook.saas.controller.AuthController.SignupRequest;
import com.khanabook.saas.entity.RestaurantProfile;
import com.khanabook.saas.entity.User;
import com.khanabook.saas.repository.RestaurantProfileRepository;
import com.khanabook.saas.repository.UserRepository;
import com.khanabook.saas.service.AuthService;
import com.khanabook.saas.utility.JwtUtility;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final UserRepository userRepository;
    private final RestaurantProfileRepository restaurantProfileRepository;
    private final JwtUtility jwtUtility;
    private final PasswordEncoder passwordEncoder;

    @Override
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getPhoneNumber())
                .orElseThrow(() -> new IllegalArgumentException("Invalid phone number or password"));

        // CRIT-03 fix: verify against BCrypt hash stored in DB
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid phone number or password");
        }

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new IllegalArgumentException("Account is disabled. Contact your administrator.");
        }

        log.info("User logged in: restaurantId={}", user.getRestaurantId());
        String token = jwtUtility.generateToken(user.getEmail(), user.getRestaurantId());
        return new AuthResponse(
                token,
                user.getRestaurantId(),
                user.getName(),
                user.getEmail(),
                user.getWhatsappNumber()
        );
    }

    @Override
    @Transactional
    public AuthResponse signup(SignupRequest request) {
        if (userRepository.findByEmail(request.getPhoneNumber()).isPresent()) {
            throw new IllegalArgumentException("An account with this phone number already exists.");
        }

        // CRIT-06 fix: use UUID-based ID to avoid modulo collisions
        Long newRestaurantId = Math.abs(UUID.randomUUID().getMostSignificantBits());

        // Create Restaurant Profile
        RestaurantProfile profile = new RestaurantProfile();
        profile.setRestaurantId(newRestaurantId);
        profile.setDeviceId(request.getDeviceId());
        profile.setLocalId(1);
        profile.setShopName(request.getName() + "'s Restaurant");
        profile.setUpdatedAt(System.currentTimeMillis());
        profile.setServerUpdatedAt(System.currentTimeMillis());
        restaurantProfileRepository.save(profile);

        // CRIT-03 fix: hash the password with BCrypt before storing
        String hashedPassword = passwordEncoder.encode(request.getPassword());

        // Create Admin User
        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getPhoneNumber());
        user.setWhatsappNumber(request.getPhoneNumber());
        user.setPasswordHash(hashedPassword);
        user.setRestaurantId(newRestaurantId);
        user.setDeviceId(request.getDeviceId());
        user.setLocalId(1);
        user.setIsActive(true);
        user.setUpdatedAt(System.currentTimeMillis());
        user.setServerUpdatedAt(System.currentTimeMillis());
        user.setCreatedAt(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        userRepository.save(user);

        log.info("New user signed up: restaurantId={}", newRestaurantId);
        String token = jwtUtility.generateToken(user.getEmail(), newRestaurantId);
        return new AuthResponse(
                token,
                newRestaurantId,
                user.getName(),
                user.getEmail(),
                user.getWhatsappNumber()
        );
    }

    @Override
    @Transactional
    public AuthResponse googleLogin(com.khanabook.saas.controller.AuthController.GoogleLoginRequest request) {
        try {
            com.google.api.client.http.HttpTransport transport = new com.google.api.client.http.javanet.NetHttpTransport();
            com.google.api.client.json.JsonFactory jsonFactory = new com.google.api.client.json.gson.GsonFactory();
            com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier verifier = new com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier.Builder(transport, jsonFactory)
                    .build();

            com.google.api.client.googleapis.auth.oauth2.GoogleIdToken idToken = verifier.verify(request.getIdToken());
            if (idToken != null) {
                com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload payload = idToken.getPayload();
                String email = payload.getEmail();
                String name = (String) payload.get("name");

                return userRepository.findByEmail(email).map(user -> {
                    if (!Boolean.TRUE.equals(user.getIsActive())) {
                        throw new IllegalArgumentException("Account is disabled. Contact your administrator.");
                    }
                    String token = jwtUtility.generateToken(user.getEmail(), user.getRestaurantId());
                    return new AuthResponse(
                            token,
                            user.getRestaurantId(),
                            user.getName(),
                            user.getEmail(),
                            user.getWhatsappNumber()
                    );
                }).orElseGet(() -> {
                    // Create new user if not exists
                    Long newRestaurantId = Math.abs(UUID.randomUUID().getMostSignificantBits());
                    
                    RestaurantProfile profile = new RestaurantProfile();
                    profile.setRestaurantId(newRestaurantId);
                    profile.setDeviceId(request.getDeviceId());
                    profile.setLocalId(1);
                    profile.setShopName((name != null ? name : "User") + "'s Restaurant");
                    profile.setUpdatedAt(System.currentTimeMillis());
                    profile.setServerUpdatedAt(System.currentTimeMillis());
                    restaurantProfileRepository.save(profile);

                    User user = new User();
                    user.setName(name != null ? name : "Google User");
                    user.setEmail(email);
                    user.setWhatsappNumber(email);
                    user.setPasswordHash("GOOGLE_AUTH"); // Indicate Google auth
                    user.setRestaurantId(newRestaurantId);
                    user.setDeviceId(request.getDeviceId());
                    user.setLocalId(1);
                    user.setIsActive(true);
                    user.setUpdatedAt(System.currentTimeMillis());
                    user.setServerUpdatedAt(System.currentTimeMillis());
                    user.setCreatedAt(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
                    userRepository.save(user);

                    String token = jwtUtility.generateToken(user.getEmail(), newRestaurantId);
                    return new AuthResponse(
                            token,
                            newRestaurantId,
                            user.getName(),
                            user.getEmail(),
                            user.getWhatsappNumber()
                    );
                });
            } else {
                throw new IllegalArgumentException("Invalid Google ID token.");
            }
        } catch (Exception e) {
            log.error("Google login failed", e);
            throw new IllegalArgumentException("Google login failed: " + e.getMessage());
        }
    }
    @Override
    @Transactional
    public void resetPassword(String phoneNumber, String newPassword) {
        User user = userRepository.findByEmail(phoneNumber)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(System.currentTimeMillis());
        user.setServerUpdatedAt(System.currentTimeMillis());
        userRepository.save(user);
        log.info("Password reset successful for user: {}", phoneNumber);
    }

    @Override
    public boolean checkUserExists(String phoneNumber) {
        return userRepository.existsByEmail(phoneNumber);
    }
}
