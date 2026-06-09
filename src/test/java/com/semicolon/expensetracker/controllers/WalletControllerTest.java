package com.semicolon.expensetracker.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class WalletControllerTest extends BaseIntegrationTest {

    // ─── POST /api/wallets ────────────────────────────────────────────────────

    @Test
    void createWallet_authenticated_returns201WithWalletData() throws Exception {
        Map<String, Object> payload = Map.of("name", "Main Wallet", "currency", "NAIRA");
        mockMvc.perform(post("/api/wallets")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.publicId").isNotEmpty())
                .andExpect(jsonPath("$.name").value("Main Wallet"))
                .andExpect(jsonPath("$.currency").value("NAIRA"))
                .andExpect(jsonPath("$.message").value("Wallet created successfully"));
    }

    @Test
    void createWallet_noCurrencyProvided_defaultsToNaira() throws Exception {
        Map<String, Object> payload = Map.of("name", "Default Currency Wallet");
        mockMvc.perform(post("/api/wallets")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.currency").value("NAIRA"));
    }

    @Test
    void createWallet_unauthenticated_returns403() throws Exception {
        Map<String, Object> payload = Map.of("name", "Wallet");
        mockMvc.perform(post("/api/wallets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createWallet_missingName_returns400() throws Exception {
        Map<String, Object> payload = Map.of("currency", "DOLLAR");
        mockMvc.perform(post("/api/wallets")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.name").exists());
    }

    // ─── GET /api/wallets ─────────────────────────────────────────────────────

    @Test
    void getMyWallets_authenticated_returnsWalletList() throws Exception {
        createWallet("Wallet A");
        createWallet("Wallet B");

        mockMvc.perform(get("/api/wallets")
                        .header("Authorization", authHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(2))))
                .andExpect(jsonPath("$[*].name", hasItems("Wallet A", "Wallet B")));
    }

    @Test
    void getMyWallets_noWalletsExist_returnsEmptyList() throws Exception {
        mockMvc.perform(get("/api/wallets")
                        .header("Authorization", authHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getMyWallets_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/wallets"))
                .andExpect(status().isForbidden());
    }

    // ─── GET /api/wallets/{publicId}/balance ──────────────────────────────────

    @Test
    void getWalletBalance_ownWalletNoTransactions_returnsZeroBalance() throws Exception {
        String walletId = createWallet("Balance Wallet");

        mockMvc.perform(get("/api/wallets/{id}/balance", walletId)
                        .header("Authorization", authHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.walletId").value(walletId))
                .andExpect(jsonPath("$.walletName").value("Balance Wallet"))
                .andExpect(jsonPath("$.balance").value(0));
    }

    @Test
    void getWalletBalance_walletNotFound_returns404() throws Exception {
        UUID unknownId = UUID.randomUUID();
        mockMvc.perform(get("/api/wallets/{id}/balance", unknownId)
                        .header("Authorization", authHeader()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Wallet does not exist"));
    }

    @Test
    void getWalletBalance_walletBelongsToOtherUser_returns403() throws Exception {
        // Create wallet as user1
        String walletId = createWallet("User1 Wallet");

        // Login as user2 and try to get balance
        String token2 = createSecondUserAndGetToken();

        mockMvc.perform(get("/api/wallets/{id}/balance", walletId)
                        .header("Authorization", "Bearer " + token2))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Wallet does not belong to this user"));
    }

    @Test
    void getWalletBalance_unauthenticated_returns403() throws Exception {
        String walletId = createWallet("Some Wallet");
        mockMvc.perform(get("/api/wallets/{id}/balance", walletId))
                .andExpect(status().isForbidden());
    }
}
