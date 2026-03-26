package com.khanabook.saas.security;

import com.khanabook.saas.BaseIntegrationTest;
import com.khanabook.saas.utility.JwtUtility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
public class SpringRoleTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtility jwtUtility;

    @Test
    void unauthenticatedRequestToSync_returns403() throws Exception {
        mockMvc.perform(get("/sync/restaurantprofile/pull")
                .param("lastSyncTimestamp", "0")
                .param("deviceId", "dev1"))
                .andExpect(status().isForbidden());
    }

    @Test
    void ownerPushesProfileWithOwnRestaurantId_returns200() throws Exception {
        String token = jwtUtility.generateToken("owner@test.com", 1L, "OWNER");
        
        String json = "[{\"localId\": 1, \"restaurantId\": 1, \"deviceId\": \"dev1\", \"shopName\": \"My Shop\", \"updatedAt\": " + System.currentTimeMillis() + "}]";

        mockMvc.perform(post("/sync/restaurantprofile/push")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk());
    }

    @Test
    void ownerPushesProfileWithDifferentRestaurantId_returns403() throws Exception {
        String token = jwtUtility.generateToken("owner@test.com", 1L, "OWNER");
        
        // Push for restaurantId 2 while being owner of restaurantId 1
        String json = "[{\"localId\": 1, \"restaurantId\": 2, \"deviceId\": \"dev1\", \"shopName\": \"Evil Shop\", \"updatedAt\": " + System.currentTimeMillis() + "}]";

        mockMvc.perform(post("/sync/restaurantprofile/push")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isForbidden());
    }

    @Test
    void ownerCallsAdminEndpoint_returns403() throws Exception {
        String token = jwtUtility.generateToken("owner@test.com", 1L, "OWNER");

        mockMvc.perform(get("/admin/anything")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void kbookAdminPushesProfileForAnyRestaurantId_returns200() throws Exception {
        String token = jwtUtility.generateToken("admin@test.com", null, "KBOOK_ADMIN");
        
        String json = "[{\"localId\": 1, \"restaurantId\": 99, \"deviceId\": \"dev1\", \"shopName\": \"Admin Shop\", \"updatedAt\": " + System.currentTimeMillis() + "}]";

        mockMvc.perform(post("/sync/restaurantprofile/push")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk());
    }

    @Test
    void kbookAdminCallsMasterSyncWithSpecificRestaurantId_returns200() throws Exception {
        String token = jwtUtility.generateToken("admin@test.com", null, "KBOOK_ADMIN");

        mockMvc.perform(get("/sync/master/pull")
                .header("Authorization", "Bearer " + token)
                .param("lastSyncTimestamp", "0")
                .param("deviceId", "dev1")
                .param("restaurantId", "123"))
                .andExpect(status().isOk());
    }
}
