package com.semicolon.expensetracker.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class RecurringExpenseControllerTest extends BaseIntegrationTest {

    private String walletId;
    private String categoryId;

    @BeforeEach
    void setUpWalletAndCategory() throws Exception {
        walletId   = createWallet("Recurring Wallet");
        categoryId = createCategory("Subscription", "OUTFLOW_FIXED_COST");
    }

    // ─── POST /api/recurring-expenses ─────────────────────────────────────────

    @Test
    void create_validRequest_returns201WithRecurringExpenseData() throws Exception {
        Map<String, Object> payload = Map.of(
                "walletId", walletId,
                "categoryId", categoryId,
                "amount", 4999,
                "frequency", "MONTHLY",
                "nextDueDate", "2026-07-01"
        );
        mockMvc.perform(post("/api/recurring-expenses")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.publicId").isNotEmpty())
                .andExpect(jsonPath("$.amount").value(4999))
                .andExpect(jsonPath("$.frequency").value("MONTHLY"))
                .andExpect(jsonPath("$.walletId").value(walletId))
                .andExpect(jsonPath("$.categoryId").value(categoryId))
                .andExpect(jsonPath("$.nextDueDate").value("2026-07-01"))
                .andExpect(jsonPath("$.message").value("Success"));
    }

    @Test
    void create_withoutNextDueDate_defaultsToToday() throws Exception {
        Map<String, Object> payload = Map.of(
                "walletId", walletId,
                "categoryId", categoryId,
                "amount", 2000,
                "frequency", "WEEKLY"
        );
        mockMvc.perform(post("/api/recurring-expenses")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nextDueDate").isNotEmpty());
    }

    @Test
    void create_walletNotFound_returns404() throws Exception {
        Map<String, Object> payload = Map.of(
                "walletId", UUID.randomUUID().toString(),
                "categoryId", categoryId,
                "amount", 1000,
                "frequency", "MONTHLY"
        );
        mockMvc.perform(post("/api/recurring-expenses")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Wallet does not exist"));
    }

    @Test
    void create_categoryNotFound_returns404() throws Exception {
        Map<String, Object> payload = Map.of(
                "walletId", walletId,
                "categoryId", UUID.randomUUID().toString(),
                "amount", 1000,
                "frequency", "MONTHLY"
        );
        mockMvc.perform(post("/api/recurring-expenses")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Category does not exist"));
    }

    @Test
    void create_walletBelongsToOtherUser_returns403() throws Exception {
        String token2 = createSecondUserAndGetToken();
        Map<String, Object> payload = Map.of(
                "walletId", walletId,        // user1's wallet
                "categoryId", categoryId,
                "amount", 1000,
                "frequency", "MONTHLY"
        );
        mockMvc.perform(post("/api/recurring-expenses")
                        .header("Authorization", "Bearer " + token2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Wallet does not belong to this user"));
    }

    @Test
    void create_missingAmount_returns400() throws Exception {
        Map<String, Object> payload = Map.of(
                "walletId", walletId,
                "categoryId", categoryId,
                "frequency", "MONTHLY"
        );
        mockMvc.perform(post("/api/recurring-expenses")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_missingFrequency_returns400() throws Exception {
        Map<String, Object> payload = Map.of(
                "walletId", walletId,
                "categoryId", categoryId,
                "amount", 1000
        );
        mockMvc.perform(post("/api/recurring-expenses")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_unauthenticated_returns403() throws Exception {
        Map<String, Object> payload = Map.of(
                "walletId", walletId, "categoryId", categoryId,
                "amount", 1000, "frequency", "MONTHLY"
        );
        mockMvc.perform(post("/api/recurring-expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isForbidden());
    }

    // ─── GET /api/recurring-expenses ─────────────────────────────────────────

    @Test
    void getMyRecurringExpenses_authenticated_returnsList() throws Exception {
        createRecurringViaApi(walletId, categoryId, 5000, "MONTHLY");

        mockMvc.perform(get("/api/recurring-expenses")
                        .header("Authorization", authHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].frequency").value("MONTHLY"));
    }

    @Test
    void getMyRecurringExpenses_noExpenses_returnsEmptyList() throws Exception {
        mockMvc.perform(get("/api/recurring-expenses")
                        .header("Authorization", authHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getMyRecurringExpenses_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/recurring-expenses"))
                .andExpect(status().isForbidden());
    }

    // ─── DELETE /api/recurring-expenses/{publicId} ────────────────────────────

    @Test
    void delete_ownRecurringExpense_returns200() throws Exception {
        String recurringId = createRecurringViaApi(walletId, categoryId, 3000, "WEEKLY");

        mockMvc.perform(delete("/api/recurring-expenses/{id}", recurringId)
                        .header("Authorization", authHeader()))
                .andExpect(status().isOk())
                .andExpect(content().string("Recurring expense deleted successfully"));
    }

    @Test
    void delete_recurringExpenseNotFound_returns404() throws Exception {
        mockMvc.perform(delete("/api/recurring-expenses/{id}", UUID.randomUUID())
                        .header("Authorization", authHeader()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Recurring expense does not exist"));
    }

    @Test
    void delete_recurringExpenseBelongsToOtherUser_returns403() throws Exception {
        // user1 creates a recurring expense
        String recurringId = createRecurringViaApi(walletId, categoryId, 1500, "MONTHLY");

        // user2 tries to delete it
        String token2 = createSecondUserAndGetToken();
        mockMvc.perform(delete("/api/recurring-expenses/{id}", recurringId)
                        .header("Authorization", "Bearer " + token2))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Recurring expense does not belong to this user"));
    }

    @Test
    void delete_unauthenticated_returns403() throws Exception {
        String recurringId = createRecurringViaApi(walletId, categoryId, 2000, "DAILY");
        mockMvc.perform(delete("/api/recurring-expenses/{id}", recurringId))
                .andExpect(status().isForbidden());
    }

    // ─── Helper ───────────────────────────────────────────────────────────────

    private String createRecurringViaApi(String walletId, String categoryId,
                                         int amount, String frequency) throws Exception {
        Map<String, Object> payload = Map.of(
                "walletId", walletId,
                "categoryId", categoryId,
                "amount", amount,
                "frequency", frequency
        );
        String body = mockMvc.perform(post("/api/recurring-expenses")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("publicId").asText();
    }
}
