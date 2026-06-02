package com.semicolon.expensetracker.controllers;

import com.semicolon.expensetracker.dtos.request.CreateWalletRequest;
import com.semicolon.expensetracker.services.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/wallets")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @PostMapping
    public ResponseEntity<?> createWallet(@Valid @RequestBody CreateWalletRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(walletService.createWallet(request));
    }

    @GetMapping
    public ResponseEntity<?> getMyWallets() {
        return ResponseEntity.ok(walletService.getWalletsByUser());
    }

    @GetMapping("/{walletId}/balance")
    public ResponseEntity<?> getWalletBalance(@PathVariable UUID walletId) {
        return ResponseEntity.ok(walletService.getWalletBalance(walletId));
    }
}