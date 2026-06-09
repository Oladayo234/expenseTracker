package com.semicolon.expensetracker.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.Map;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public abstract class BaseIntegrationTest {

    // ─── Injected beans ───────────────────────────────────────────────────────

    @Autowired
    private WebApplicationContext context;

    protected final ObjectMapper objectMapper = new ObjectMapper();

    // ─── Per-test state ───────────────────────────────────────────────────────

    protected MockMvc mockMvc;
    protected String token;
    protected String testUsername;
    protected String testEmail;
    protected static final String DEFAULT_PASSWORD = "Password1!";

    // ─── Setup ───────────────────────────────────────────────────────────────

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        String uid = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        testUsername = "user_" + uid;
        testEmail    = "user_" + uid + "@test.com";
        registerUser(testUsername, testEmail, DEFAULT_PASSWORD);
        token = loginAndGetToken(testUsername, DEFAULT_PASSWORD);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    protected void registerUser(String username, String email, String password) throws Exception {
        Map<String, Object> payload = Map.of(
                "userName", username,
                "name", "Integration Tester",
                "email", email,
                "password", password,
                "phoneNumber", "08012345678"
        );
        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated());
    }

    protected String loginAndGetToken(String username, String password) throws Exception {
        String body = mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("userName", username, "password", password))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("token").asText();
    }

    protected String authHeader() {
        return "Bearer " + token;
    }

    /** Creates a wallet for the current test user and returns its publicId. */
    protected String createWallet(String name) throws Exception {
        String body = mockMvc.perform(post("/api/wallets")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", name))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("publicId").asText();
    }

    /** Creates a category for the current test user and returns its publicId. */
    protected String createCategory(String name, String transactionType) throws Exception {
        String body = mockMvc.perform(post("/api/categories")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("name", name, "transactionType", transactionType))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("publicId").asText();
    }

    /** Registers + logs in a second user and returns their JWT token. */
    protected String createSecondUserAndGetToken() throws Exception {
        String uid = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String user2  = "user2_" + uid;
        String email2 = "user2_" + uid + "@test.com";
        registerUser(user2, email2, DEFAULT_PASSWORD);
        return loginAndGetToken(user2, DEFAULT_PASSWORD);
    }
}
