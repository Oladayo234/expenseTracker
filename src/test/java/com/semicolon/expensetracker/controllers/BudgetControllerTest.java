package com.semicolon.expensetracker.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class BudgetControllerTest extends BaseIntegrationTest {

    private String categoryId;

    @BeforeEach
    void setUpCategory() throws Exception {
        categoryId = createCategory("Rent", "OUTFLOW_FIXED_COST");
    }

    // ─── POST /api/budgets ────────────────────────────────────────────────────

    @Test
    void createBudget_validRequest_returns201WithBudgetData() throws Exception {
        Map<String, Object> payload = Map.of(
                "categoryId", categoryId,
                "budgetName", "January Rent",
                "budgetAmount", 150000,
                "yearMonth", "2026-01"
        );
        mockMvc.perform(post("/api/budgets")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.publicId").isNotEmpty())
                .andExpect(jsonPath("$.budgetName").value("January Rent"))
                .andExpect(jsonPath("$.budgetAmount").value(150000))
                .andExpect(jsonPath("$.categoryId").value(categoryId))
                .andExpect(jsonPath("$.actualSpent").value(0))
                .andExpect(jsonPath("$.amountRemaining").value(150000))
                .andExpect(jsonPath("$.message").value("Budget created successfully"));
    }

    @Test
    void createBudget_duplicateCategoryAndMonth_returns409() throws Exception {
        Map<String, Object> payload = Map.of(
                "categoryId", categoryId,
                "budgetName", "First Budget",
                "budgetAmount", 50000,
                "yearMonth", "2026-03"
        );
        // Create first budget
        mockMvc.perform(post("/api/budgets")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated());

        // Attempt duplicate
        mockMvc.perform(post("/api/budgets")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Budget already exists for this category and month"));
    }

    @Test
    void createBudget_categoryNotFound_returns404() throws Exception {
        Map<String, Object> payload = Map.of(
                "categoryId", UUID.randomUUID().toString(),
                "budgetName", "Ghost Budget",
                "budgetAmount", 10000,
                "yearMonth", "2026-02"
        );
        mockMvc.perform(post("/api/budgets")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Category does not exist"));
    }

    @Test
    void createBudget_missingBudgetAmount_returns400() throws Exception {
        Map<String, Object> payload = Map.of(
                "categoryId", categoryId,
                "budgetName", "No Amount Budget",
                "yearMonth", "2026-04"
        );
        mockMvc.perform(post("/api/budgets")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createBudget_missingYearMonth_returns400() throws Exception {
        Map<String, Object> payload = Map.of(
                "categoryId", categoryId,
                "budgetName", "No Month Budget",
                "budgetAmount", 20000
        );
        mockMvc.perform(post("/api/budgets")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createBudget_unauthenticated_returns403() throws Exception {
        Map<String, Object> payload = Map.of(
                "categoryId", categoryId,
                "budgetAmount", 10000,
                "yearMonth", "2026-05"
        );
        mockMvc.perform(post("/api/budgets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isForbidden());
    }

    // ─── GET /api/budgets ─────────────────────────────────────────────────────

    @Test
    void getMyBudgets_authenticated_returnsBudgetList() throws Exception {
        createBudgetViaApi(categoryId, "Budget A", 10000, "2026-06");

        mockMvc.perform(get("/api/budgets")
                        .header("Authorization", authHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].budgetName").value("Budget A"));
    }

    @Test
    void getMyBudgets_noBudgets_returnsEmptyList() throws Exception {
        mockMvc.perform(get("/api/budgets")
                        .header("Authorization", authHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getMyBudgets_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/budgets"))
                .andExpect(status().isForbidden());
    }

    // ─── GET /api/budgets/vs-actual ───────────────────────────────────────────

    @Test
    void getBudgetVsActual_authenticated_returnsVsActualList() throws Exception {
        createBudgetViaApi(categoryId, "Vs Actual Budget", 50000, "2026-07");

        mockMvc.perform(get("/api/budgets/vs-actual")
                        .header("Authorization", authHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].budgetAmount").value(50000))
                .andExpect(jsonPath("$[0].status").value("ON_TRACK"));
    }

    @Test
    void getBudgetVsActual_noBudgets_returnsEmptyList() throws Exception {
        mockMvc.perform(get("/api/budgets/vs-actual")
                        .header("Authorization", authHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getBudgetVsActual_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/budgets/vs-actual"))
                .andExpect(status().isForbidden());
    }

    // ─── DELETE /api/budgets/{publicId} ──────────────────────────────────────

    @Test
    void deleteBudget_ownBudget_returns200() throws Exception {
        String budgetId = createBudgetViaApi(categoryId, "ToDelete", 5000, "2026-08");

        mockMvc.perform(delete("/api/budgets/{id}", budgetId)
                        .header("Authorization", authHeader()))
                .andExpect(status().isOk())
                .andExpect(content().string("Budget deleted successfully"));
    }

    @Test
    void deleteBudget_budgetNotFound_returns404() throws Exception {
        mockMvc.perform(delete("/api/budgets/{id}", UUID.randomUUID())
                        .header("Authorization", authHeader()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Budget does not exist"));
    }

    @Test
    void deleteBudget_budgetBelongsToOtherUser_returns403() throws Exception {
        // user2 creates a budget for their own category
        String token2 = createSecondUserAndGetToken();
        String cat2Id = mockMvc.perform(post("/api/categories")
                        .header("Authorization", "Bearer " + token2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("name", "User2Cat", "transactionType", "OUTFLOW"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String catId2 = objectMapper.readTree(cat2Id).get("publicId").asText();

        Map<String, Object> budgetPayload = Map.of(
                "categoryId", catId2,
                "budgetAmount", 20000,
                "yearMonth", "2026-09"
        );
        String budgetBody = mockMvc.perform(post("/api/budgets")
                        .header("Authorization", "Bearer " + token2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(budgetPayload)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String budgetId = objectMapper.readTree(budgetBody).get("publicId").asText();

        // user1 tries to delete user2's budget
        mockMvc.perform(delete("/api/budgets/{id}", budgetId)
                        .header("Authorization", authHeader()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Budget does not belong to this user"));
    }

    @Test
    void deleteBudget_unauthenticated_returns403() throws Exception {
        String budgetId = createBudgetViaApi(categoryId, "SomeBudget", 5000, "2026-10");
        mockMvc.perform(delete("/api/budgets/{id}", budgetId))
                .andExpect(status().isForbidden());
    }

    // ─── Helper ───────────────────────────────────────────────────────────────

    private String createBudgetViaApi(String catId, String name, int amount,
                                      String yearMonth) throws Exception {
        Map<String, Object> payload = Map.of(
                "categoryId", catId,
                "budgetName", name,
                "budgetAmount", amount,
                "yearMonth", yearMonth
        );
        String body = mockMvc.perform(post("/api/budgets")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("publicId").asText();
    }
}
