package com.khanabook.saas;

import com.khanabook.saas.controller.AuthController.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.*;

/**
 * System tests — the full app runs on a random port, requests go over real HTTP.
 * This is what the Android client actually hits.
 * Tests: auth flow, JWT enforcement, rate limiting, sync endpoint shape.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class SystemTest extends BaseIntegrationTest {

    @LocalServerPort int port;
    @Autowired TestRestTemplate rest;

    // ─── Auth: signup → login → use token ────────────────────────────────────

    @Test
    void fullAuthFlow_signupThenLoginThenAccessSync() {
        String phone = uniquePhone();

        // 1. Signup
        SignupRequest signup = new SignupRequest(phone, "Nandha Kumar", "pass123", "DEVICE_1");
        ResponseEntity<AuthResponse> signupResp =
            rest.postForEntity("/api/v1/auth/signup", signup, AuthResponse.class);

        assertThat(signupResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(signupResp.getBody().getToken()).isNotBlank();
        assertThat(signupResp.getBody().getRestaurantId()).isPositive();
        Long restaurantId = signupResp.getBody().getRestaurantId();

        // 2. Login
        LoginRequest login = new LoginRequest();
        login.setPhoneNumber(phone);
        login.setPassword("pass123");
        ResponseEntity<AuthResponse> loginResp =
            rest.postForEntity("/api/v1/auth/login", login, AuthResponse.class);

        assertThat(loginResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        String token = loginResp.getBody().getToken();
        assertThat(token).isNotBlank();
        assertThat(loginResp.getBody().getRestaurantId()).isEqualTo(restaurantId);

        // 3. Use token to pull bills — must return 200 with empty list
        ResponseEntity<String> pullResp = rest.exchange(
            "/api/v1/sync/bills/pull?lastSyncTimestamp=0&deviceId=DEVICE_2",
            HttpMethod.GET, bearerRequest(token), String.class);

        assertThat(pullResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(pullResp.getBody()).contains("[]");
    }

    // ─── Auth: validation ─────────────────────────────────────────────────────

    @Test
    void signup_invalidPhoneFormat_returns400() {
        SignupRequest req = new SignupRequest("not-a-phone", "Test", "pass123", "D");
        ResponseEntity<String> resp =
            rest.postForEntity("/api/v1/auth/signup", req, String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void signup_duplicatePhone_returns400() {
        String phone = uniquePhone();
        SignupRequest req = new SignupRequest(phone, "User A", "pass123", "D");
        rest.postForEntity("/api/v1/auth/signup", req, String.class);

        // Second signup with same phone
        ResponseEntity<String> second =
            rest.postForEntity("/api/v1/auth/signup", req, String.class);
        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(second.getBody()).contains("already exists");
    }

    @Test
    void login_wrongPassword_returns400() {
        String phone = uniquePhone();
        rest.postForEntity("/api/v1/auth/signup",
            new SignupRequest(phone, "User", "correct", "D"), String.class);

        LoginRequest bad = new LoginRequest();
        bad.setPhoneNumber(phone);
        bad.setPassword("wrong");
        ResponseEntity<String> resp = rest.postForEntity("/api/v1/auth/login", bad, String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void checkUser_existingPhone_returnsTrue() {
        String phone = uniquePhone();
        rest.postForEntity("/api/v1/auth/signup",
            new SignupRequest(phone, "User", "pass123", "D"), String.class);

        ResponseEntity<Boolean> resp =
            rest.getForEntity("/api/v1/auth/check-user?phoneNumber=" + phone, Boolean.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isTrue();
    }

    @Test
    void checkUser_unknownPhone_returnsFalse() {
        ResponseEntity<Boolean> resp =
            rest.getForEntity("/api/v1/auth/check-user?phoneNumber=9999999999", Boolean.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isFalse();
    }

    // ─── JWT enforcement ──────────────────────────────────────────────────────

    @Test
    void syncEndpoint_noToken_returns401() {
        ResponseEntity<String> resp =
            rest.getForEntity("/api/v1/sync/bills/pull?lastSyncTimestamp=0&deviceId=X", String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void syncEndpoint_invalidToken_returns401() {
        ResponseEntity<String> resp = rest.exchange(
            "/api/v1/sync/bills/pull?lastSyncTimestamp=0&deviceId=X",
            HttpMethod.GET, bearerRequest("not.a.valid.token"), String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void resetPassword_noToken_returns401() {
        // reset-password is behind JWT — must not be publicly accessible
        ResponseEntity<String> resp = rest.exchange(
            "/api/v1/auth/reset-password?phoneNumber=1234567890&newPassword=x",
            HttpMethod.POST, HttpEntity.EMPTY, String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void masterSync_noToken_returns401() {
        ResponseEntity<String> resp =
            rest.getForEntity("/api/v1/sync/master/pull?lastSyncTimestamp=0&deviceId=X", String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ─── Sync endpoint shapes ─────────────────────────────────────────────────

    @Test
    void masterSync_pull_returnsAllNineCollections() {
        String token = signupAndGetToken();

        ResponseEntity<String> resp = rest.exchange(
            "/api/v1/sync/master/pull?lastSyncTimestamp=0&deviceId=OTHER_DEVICE",
            HttpMethod.GET, bearerRequest(token), String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        String body = resp.getBody();
        // All 9 collections must be present in the response JSON
        assertThat(body).contains("profiles");
        assertThat(body).contains("users");
        assertThat(body).contains("categories");
        assertThat(body).contains("menuItems");
        assertThat(body).contains("itemVariants");
        assertThat(body).contains("stockLogs");
        assertThat(body).contains("bills");
        assertThat(body).contains("billItems");
        assertThat(body).contains("billPayments");
    }

    @Test
    void billPush_emptyList_returnsEmptySuccessLists() {
        String token = signupAndGetToken();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> req = new HttpEntity<>("[]", headers);

        ResponseEntity<String> resp =
            rest.postForEntity("/api/v1/sync/bills/push", req, String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).contains("successfulLocalIds");
        assertThat(resp.getBody()).contains("failedLocalIds");
    }

    @Test
    void googleLogin_missingIdToken_returns400() {
        // @Valid + @NotBlank on GoogleLoginRequest.idToken
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> req = new HttpEntity<>("{\"deviceId\":\"D1\"}", headers);

        ResponseEntity<String> resp =
            rest.postForEntity("/api/v1/auth/google", req, String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody()).contains("idToken");
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private String signupAndGetToken() {
        String phone = uniquePhone();
        ResponseEntity<AuthResponse> resp = rest.postForEntity("/api/v1/auth/signup",
            new SignupRequest(phone, "Test User", "pass123", "DEVICE_SYS"), AuthResponse.class);
        return resp.getBody().getToken();
    }

    private HttpEntity<Void> bearerRequest(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return new HttpEntity<>(headers);
    }

    private static long phoneCounter = 7000000000L;
    private static synchronized String uniquePhone() {
        return String.valueOf(phoneCounter++);
    }
}
