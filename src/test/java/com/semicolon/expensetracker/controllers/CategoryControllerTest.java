package com.semicolon.expensetracker.controllers;

import com.semicolon.expensetracker.data.models.Category;
import com.semicolon.expensetracker.data.repositories.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CategoryControllerTest extends BaseIntegrationTest {

    @Autowired
    private CategoryRepository categoryRepository;

    // ─── POST /api/categories ─────────────────────────────────────────────────

    @Test
    void createCategory_validRequest_returns201() throws Exception {
        Map<String, Object> payload = Map.of("name", "Groceries", "transactionType", "OUTFLOW");
        mockMvc.perform(post("/api/categories")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Groceries"))
                .andExpect(jsonPath("$.publicId").isNotEmpty())
                .andExpect(jsonPath("$.transactionType").value("OUTFLOW"));
    }

    @Test
    void createCategory_duplicateName_returns409() throws Exception {
        createCategory("Transport", "OUTFLOW");

        Map<String, Object> payload = Map.of("name", "Transport", "transactionType", "OUTFLOW");
        mockMvc.perform(post("/api/categories")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", containsString("Transport")));
    }

    @Test
    void createCategory_missingName_returns400() throws Exception {
        Map<String, Object> payload = Map.of("transactionType", "OUTFLOW");
        mockMvc.perform(post("/api/categories")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.name").exists());
    }

    @Test
    void createCategory_missingTransactionType_returns400() throws Exception {
        Map<String, Object> payload = Map.of("name", "SomeCat");
        mockMvc.perform(post("/api/categories")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createCategory_unauthenticated_returns403() throws Exception {
        Map<String, Object> payload = Map.of("name", "X", "transactionType", "OUTFLOW");
        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isForbidden());
    }

    // ─── GET /api/categories ──────────────────────────────────────────────────

    @Test
    void getCategories_authenticated_returnsUserCategories() throws Exception {
        createCategory("Dining", "OUTFLOW");
        createCategory("Salary", "INFLOW");

        mockMvc.perform(get("/api/categories")
                        .header("Authorization", authHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[*].name", hasItems("Dining", "Salary")));
    }

    @Test
    void getCategories_noCustomCategories_returnsDefaultsOnly() throws Exception {
        mockMvc.perform(get("/api/categories")
                        .header("Authorization", authHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getCategories_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isForbidden());
    }

    // ─── DELETE /api/categories ───────────────────────────────────────────────

    @Test
    void deleteCategory_ownCustomCategory_returns200() throws Exception {
        String categoryId = createCategory("ToDelete", "OUTFLOW");

        Map<String, String> payload = Map.of("categoryId", categoryId);
        mockMvc.perform(delete("/api/categories")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(content().string("Category deleted successfully"));
    }

    @Test
    void deleteCategory_defaultCategory_returns400() throws Exception {
        // Insert a default category directly via repository (no API creates defaults)
        Category defaultCat = new Category();
        defaultCat.setName("Uncategorized_Test");
        defaultCat.setDefaultCategory(true);
        defaultCat.setUser(null);
        Category saved = categoryRepository.save(defaultCat);
        categoryRepository.flush();

        Map<String, String> payload = Map.of("categoryId", saved.getPublicId().toString());
        mockMvc.perform(delete("/api/categories")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Cannot delete default categories"));
    }

    @Test
    void deleteCategory_categoryNotFound_returns404() throws Exception {
        Map<String, String> payload = Map.of("categoryId", java.util.UUID.randomUUID().toString());
        mockMvc.perform(delete("/api/categories")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Category does not exist"));
    }

    @Test
    void deleteCategory_categoryBelongsToOtherUser_returns403() throws Exception {
        // user2 creates a category
        String token2 = createSecondUserAndGetToken();
        String body = mockMvc.perform(post("/api/categories")
                        .header("Authorization", "Bearer " + token2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("name", "User2Cat", "transactionType", "OUTFLOW"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String categoryId = objectMapper.readTree(body).get("publicId").asText();

        // user1 tries to delete it
        Map<String, String> payload = Map.of("categoryId", categoryId);
        mockMvc.perform(delete("/api/categories")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error", containsString("permission")));
    }

    @Test
    void deleteCategory_missingCategoryId_returns400() throws Exception {
        mockMvc.perform(delete("/api/categories")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteCategory_unauthenticated_returns403() throws Exception {
        String categoryId = createCategory("SomeCat", "OUTFLOW");
        Map<String, String> payload = Map.of("categoryId", categoryId);
        mockMvc.perform(delete("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isForbidden());
    }
}
