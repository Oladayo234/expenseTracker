package com.semicolon.expensetracker.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ExpenseControllerTest extends BaseIntegrationTest {

    private String walletId;
    private String categoryId;

    @BeforeEach
    void setUpWalletAndCategory() throws Exception {
        walletId   = createWallet("Test Wallet");
        categoryId = createCategory("Food", "OUTFLOW");
    }

    // ─── POST /api/expenses ───────────────────────────────────────────────────

    @Test
    void addExpense_validRequest_returns201WithExpenseData() throws Exception {
        Map<String, Object> payload = Map.of(
                "walletId", walletId,
                "categoryId", categoryId,
                "amount", 2500,
                "note", "Lunch",
                "paymentMethod", "CASH"
        );
        mockMvc.perform(post("/api/expenses")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.publicId").isNotEmpty())
                .andExpect(jsonPath("$.walletId").value(walletId))
                .andExpect(jsonPath("$.categoryId").value(categoryId))
                .andExpect(jsonPath("$.amount").value(2500))
                .andExpect(jsonPath("$.note").value("Lunch"))
                .andExpect(jsonPath("$.message").value("Expense added successfully"));
    }

    @Test
    void addExpense_withExplicitDate_usesProvidedDate() throws Exception {
        Map<String, Object> payload = Map.of(
                "walletId", walletId,
                "categoryId", categoryId,
                "amount", 500,
                "paymentMethod", "TRANSFER",
                "expenseDate", "2026-01-10T08:30:00"
        );
        mockMvc.perform(post("/api/expenses")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.expenseDate").value(containsString("2026-01-10")));
    }

    @Test
    void addExpense_walletNotFound_returns404() throws Exception {
        Map<String, Object> payload = Map.of(
                "walletId", UUID.randomUUID().toString(),
                "categoryId", categoryId,
                "amount", 100,
                "paymentMethod", "CASH"
        );
        mockMvc.perform(post("/api/expenses")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Wallet does not exist"));
    }

    @Test
    void addExpense_categoryNotFound_returns404() throws Exception {
        Map<String, Object> payload = Map.of(
                "walletId", walletId,
                "categoryId", UUID.randomUUID().toString(),
                "amount", 100,
                "paymentMethod", "CASH"
        );
        mockMvc.perform(post("/api/expenses")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Category does not exist"));
    }

    @Test
    void addExpense_walletBelongsToOtherUser_returns403() throws Exception {
        String token2 = createSecondUserAndGetToken();
        // user2 tries to add expense to user1's wallet
        Map<String, Object> payload = Map.of(
                "walletId", walletId,
                "categoryId", categoryId,
                "amount", 100,
                "paymentMethod", "CASH"
        );
        mockMvc.perform(post("/api/expenses")
                        .header("Authorization", "Bearer " + token2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isForbidden());
    }

    @Test
    void addExpense_missingAmount_returns400() throws Exception {
        Map<String, Object> payload = Map.of(
                "walletId", walletId,
                "categoryId", categoryId,
                "paymentMethod", "CASH"
        );
        mockMvc.perform(post("/api/expenses")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addExpense_unauthenticated_returns403() throws Exception {
        Map<String, Object> payload = Map.of(
                "walletId", walletId, "categoryId", categoryId,
                "amount", 100, "paymentMethod", "CASH"
        );
        mockMvc.perform(post("/api/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isForbidden());
    }

    // ─── GET /api/expenses/wallet/{publicId} ──────────────────────────────────

    @Test
    void getExpensesByWallet_ownWallet_returnsExpenses() throws Exception {
        // Add an expense first
        addExpenseViaApi(walletId, categoryId, 1000);

        mockMvc.perform(get("/api/expenses/wallet/{id}", walletId)
                        .header("Authorization", authHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    void getExpensesByWallet_emptyWallet_returnsEmptyList() throws Exception {
        mockMvc.perform(get("/api/expenses/wallet/{id}", walletId)
                        .header("Authorization", authHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getExpensesByWallet_walletNotFound_returns404() throws Exception {
        mockMvc.perform(get("/api/expenses/wallet/{id}", UUID.randomUUID())
                        .header("Authorization", authHeader()))
                .andExpect(status().isNotFound());
    }

    @Test
    void getExpensesByWallet_walletBelongsToOtherUser_returns403() throws Exception {
        String token2 = createSecondUserAndGetToken();
        mockMvc.perform(get("/api/expenses/wallet/{id}", walletId)
                        .header("Authorization", "Bearer " + token2))
                .andExpect(status().isForbidden());
    }

    @Test
    void getExpensesByWallet_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/expenses/wallet/{id}", walletId))
                .andExpect(status().isForbidden());
    }

    // ─── GET /api/expenses/wallet/{publicId}/range ────────────────────────────

    @Test
    void getExpensesByDateRange_ownWallet_returnsExpensesInRange() throws Exception {
        addExpenseViaApi(walletId, categoryId, 3000);

        mockMvc.perform(get("/api/expenses/wallet/{id}/range", walletId)
                        .header("Authorization", authHeader())
                        .param("start", "2026-01-01T00:00:00")
                        .param("end", "2026-12-31T23:59:59"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getExpensesByDateRange_walletNotFound_returns404() throws Exception {
        mockMvc.perform(get("/api/expenses/wallet/{id}/range", UUID.randomUUID())
                        .header("Authorization", authHeader())
                        .param("start", "2026-01-01T00:00:00")
                        .param("end", "2026-12-31T23:59:59"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getExpensesByDateRange_walletBelongsToOtherUser_returns403() throws Exception {
        String token2 = createSecondUserAndGetToken();
        mockMvc.perform(get("/api/expenses/wallet/{id}/range", walletId)
                        .header("Authorization", "Bearer " + token2)
                        .param("start", "2026-01-01T00:00:00")
                        .param("end", "2026-12-31T23:59:59"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getExpensesByDateRange_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/expenses/wallet/{id}/range", walletId)
                        .param("start", "2026-01-01T00:00:00")
                        .param("end", "2026-12-31T23:59:59"))
                .andExpect(status().isForbidden());
    }

    // ─── GET /api/expenses/category/{publicId} ────────────────────────────────

    @Test
    void getExpensesByCategory_ownCategory_returnsExpenses() throws Exception {
        addExpenseViaApi(walletId, categoryId, 500);

        mockMvc.perform(get("/api/expenses/category/{id}", categoryId)
                        .header("Authorization", authHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    void getExpensesByCategory_categoryNotFound_returns404() throws Exception {
        mockMvc.perform(get("/api/expenses/category/{id}", UUID.randomUUID())
                        .header("Authorization", authHeader()))
                .andExpect(status().isNotFound());
    }

    @Test
    void getExpensesByCategory_categoryBelongsToOtherUser_returns403() throws Exception {
        String token2 = createSecondUserAndGetToken();
        mockMvc.perform(get("/api/expenses/category/{id}", categoryId)
                        .header("Authorization", "Bearer " + token2))
                .andExpect(status().isForbidden());
    }

    @Test
    void getExpensesByCategory_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/expenses/category/{id}", categoryId))
                .andExpect(status().isForbidden());
    }

    // ─── DELETE /api/expenses/{publicId} ─────────────────────────────────────

    @Test
    void deleteExpense_ownExpense_returns200() throws Exception {
        String expenseId = addExpenseViaApi(walletId, categoryId, 750);

        mockMvc.perform(delete("/api/expenses/{id}", expenseId)
                        .header("Authorization", authHeader()))
                .andExpect(status().isOk())
                .andExpect(content().string("Expense deleted successfully"));
    }

    @Test
    void deleteExpense_expenseNotFound_returns404() throws Exception {
        mockMvc.perform(delete("/api/expenses/{id}", UUID.randomUUID())
                        .header("Authorization", authHeader()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Expense does not exist"));
    }

    @Test
    void deleteExpense_expenseBelongsToOtherUser_returns403() throws Exception {
        String expenseId = addExpenseViaApi(walletId, categoryId, 200);
        String token2 = createSecondUserAndGetToken();

        mockMvc.perform(delete("/api/expenses/{id}", expenseId)
                        .header("Authorization", "Bearer " + token2))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteExpense_unauthenticated_returns403() throws Exception {
        String expenseId = addExpenseViaApi(walletId, categoryId, 100);
        mockMvc.perform(delete("/api/expenses/{id}", expenseId))
                .andExpect(status().isForbidden());
    }

    // ─── GET /api/expenses/summary ────────────────────────────────────────────

    @Test
    void getMonthlySummary_authenticated_returnsSummary() throws Exception {
        addExpenseViaApi(walletId, categoryId, 5000);

        mockMvc.perform(get("/api/expenses/summary")
                        .header("Authorization", authHeader())
                        .param("year", "2026")
                        .param("month", "6"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.year").value(2026))
                .andExpect(jsonPath("$.month").value(6))
                .andExpect(jsonPath("$.totalIncome").exists())
                .andExpect(jsonPath("$.totalExpenses").exists())
                .andExpect(jsonPath("$.netAmount").exists());
    }

    @Test
    void getMonthlySummary_noExpenses_returnsAllZeros() throws Exception {
        mockMvc.perform(get("/api/expenses/summary")
                        .header("Authorization", authHeader())
                        .param("year", "2025")
                        .param("month", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIncome").value(0))
                .andExpect(jsonPath("$.totalExpenses").value(0))
                .andExpect(jsonPath("$.netAmount").value(0));
    }

    @Test
    void getMonthlySummary_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/expenses/summary")
                        .param("year", "2026")
                        .param("month", "1"))
                .andExpect(status().isForbidden());
    }

    // ─── GET /api/expenses/breakdown ─────────────────────────────────────────

    @Test
    void getCategoryBreakdown_authenticated_returnsBreakdown() throws Exception {
        mockMvc.perform(get("/api/expenses/breakdown")
                        .header("Authorization", authHeader())
                        .param("year", "2026")
                        .param("month", "6"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getCategoryBreakdown_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/expenses/breakdown")
                        .param("year", "2026")
                        .param("month", "1"))
                .andExpect(status().isForbidden());
    }

    // ─── Helper ───────────────────────────────────────────────────────────────

    private String addExpenseViaApi(String walletId, String categoryId, int amount) throws Exception {
        Map<String, Object> payload = Map.of(
                "walletId", walletId,
                "categoryId", categoryId,
                "amount", amount,
                "paymentMethod", "CASH"
        );
        String body = mockMvc.perform(post("/api/expenses")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("publicId").asText();
    }
}
