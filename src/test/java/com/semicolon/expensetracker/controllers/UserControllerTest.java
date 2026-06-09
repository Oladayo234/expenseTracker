package com.semicolon.expensetracker.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class UserControllerTest extends BaseIntegrationTest {

    // ─── POST /api/users/register ─────────────────────────────────────────────

    @Test
    void register_validRequest_returns201WithEmailAndUsername() throws Exception {
        String uid = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        Map<String, Object> payload = Map.of(
                "userName", "newuser_" + uid,
                "name", "New User",
                "email", "new_" + uid + "@test.com",
                "password", "securepass",
                "phoneNumber", "08099887766"
        );
        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("new_" + uid + "@test.com"))
                .andExpect(jsonPath("$.userName").value("newuser_" + uid))
                .andExpect(jsonPath("$.name").value("New User"));
    }

    @Test
    void register_duplicateEmail_returns409() throws Exception {
        // testEmail is already registered in @BeforeEach
        Map<String, Object> payload = Map.of(
                "userName", "uniqueuser_x",
                "name", "Dup Email",
                "email", testEmail,
                "password", "securepass",
                "phoneNumber", "08012345678"
        );
        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Email already in use"));
    }

    @Test
    void register_duplicateUsername_returns409() throws Exception {
        // testUsername is already registered in @BeforeEach
        Map<String, Object> payload = Map.of(
                "userName", testUsername,
                "name", "Dup User",
                "email", "unique_" + UUID.randomUUID() + "@test.com",
                "password", "securepass",
                "phoneNumber", "08012345678"
        );
        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Username already taken"));
    }

    @Test
    void register_missingPassword_returns400() throws Exception {
        Map<String, Object> payload = Map.of(
                "userName", "someuser",
                "name", "Test",
                "email", "some@test.com",
                "phoneNumber", "08012345678"
        );
        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.password").exists());
    }

    @Test
    void register_invalidEmail_returns400() throws Exception {
        Map<String, Object> payload = Map.of(
                "userName", "someuser2",
                "name", "Test",
                "email", "not-an-email",
                "password", "securepass",
                "phoneNumber", "08012345678"
        );
        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_missingName_returns400() throws Exception {
        Map<String, Object> payload = Map.of(
                "userName", "someuser3",
                "email", "some3@test.com",
                "password", "securepass",
                "phoneNumber", "08012345678"
        );
        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());
    }

    // ─── POST /api/users/login ────────────────────────────────────────────────

    @Test
    void login_validCredentials_returns200WithToken() throws Exception {
        Map<String, String> payload = Map.of("userName", testUsername, "password", DEFAULT_PASSWORD);
        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.userName").value(testUsername))
                .andExpect(jsonPath("$.message").value("Login successful"));
    }

    @Test
    void login_wrongPassword_returns401() throws Exception {
        Map<String, String> payload = Map.of("userName", testUsername, "password", "wrongpassword");
        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid credentials"));
    }

    @Test
    void login_unknownUsername_returns401() throws Exception {
        Map<String, String> payload = Map.of("userName", "ghostuser_xyz", "password", "anypass");
        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_missingUsername_returns400() throws Exception {
        Map<String, String> payload = Map.of("password", DEFAULT_PASSWORD);
        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_missingPassword_returns400() throws Exception {
        Map<String, String> payload = Map.of("userName", testUsername);
        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());
    }

    // ─── PATCH /api/users ─────────────────────────────────────────────────────

    @Test
    void updateUser_authenticated_returns200WithNewToken() throws Exception {
        String uid = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        Map<String, String> payload = Map.of("email", "updated_" + uid + "@test.com");
        mockMvc.perform(patch("/api/users")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.publicId").isNotEmpty());
    }

    @Test
    void updateUser_changeUsername_returns200() throws Exception {
        String newUsername = "updated_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        Map<String, String> payload = Map.of("userName", newUsername);
        mockMvc.perform(patch("/api/users")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(newUsername));
    }

    @Test
    void updateUser_unauthenticated_returns403() throws Exception {
        Map<String, String> payload = Map.of("email", "new@test.com");
        mockMvc.perform(patch("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateUser_invalidEmail_returns400() throws Exception {
        Map<String, String> payload = Map.of("email", "not-valid");
        mockMvc.perform(patch("/api/users")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());
    }

    // ─── PATCH /api/users/change-password ─────────────────────────────────────

    @Test
    void changePassword_validRequest_returns200() throws Exception {
        Map<String, String> payload = Map.of(
                "currentPassword", DEFAULT_PASSWORD,
                "newPassword", "NewSecure1!",
                "confirmPassword", "NewSecure1!"
        );
        mockMvc.perform(patch("/api/users/change-password")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk());
    }

    @Test
    void changePassword_wrongCurrentPassword_returns401() throws Exception {
        Map<String, String> payload = Map.of(
                "currentPassword", "WrongOldPass!",
                "newPassword", "NewSecure1!",
                "confirmPassword", "NewSecure1!"
        );
        mockMvc.perform(patch("/api/users/change-password")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Current password is incorrect"));
    }

    @Test
    void changePassword_newPasswordsDoNotMatch_returns400() throws Exception {
        Map<String, String> payload = Map.of(
                "currentPassword", DEFAULT_PASSWORD,
                "newPassword", "NewSecure1!",
                "confirmPassword", "DifferentPass1!"
        );
        mockMvc.perform(patch("/api/users/change-password")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("New passwords do not match"));
    }

    @Test
    void changePassword_newPasswordTooShort_returns400() throws Exception {
        Map<String, String> payload = Map.of(
                "currentPassword", DEFAULT_PASSWORD,
                "newPassword", "short",
                "confirmPassword", "short"
        );
        mockMvc.perform(patch("/api/users/change-password")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void changePassword_unauthenticated_returns403() throws Exception {
        Map<String, String> payload = Map.of(
                "currentPassword", DEFAULT_PASSWORD,
                "newPassword", "NewSecure1!",
                "confirmPassword", "NewSecure1!"
        );
        mockMvc.perform(patch("/api/users/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isForbidden());
    }
}
