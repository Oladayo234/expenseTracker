package com.semicolon.expensetracker.controllers;

import com.semicolon.expensetracker.dtos.request.AddExpenseRequest;
import com.semicolon.expensetracker.services.ExpenseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/expenses")
@RequiredArgsConstructor
public class ExpenseController {
    private final ExpenseService expenseService;

    @PostMapping
    public ResponseEntity<?> addExpense(@Valid @RequestBody AddExpenseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(expenseService.addExpense(request));
    }

    @GetMapping("/wallet/{walletId}")
    public ResponseEntity<?> getExpensesByWallet(@PathVariable UUID walletId, @RequestParam UUID userId) {
        return ResponseEntity.ok(expenseService.getExpensesByWallet(walletId, userId));
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<?> getExpensesByCategory(@PathVariable UUID categoryId, @RequestParam UUID userId) {
        return ResponseEntity.ok(expenseService.getExpensesByCategory(categoryId, userId));
    }

    @DeleteMapping("/{expenseId}")
    public ResponseEntity<?> deleteExpense(@PathVariable UUID expenseId, @RequestParam UUID userId) {
        expenseService.deleteExpense(expenseId, userId);
        return ResponseEntity.ok("Expense deleted successfully");
    }

    @GetMapping("/summary")
    public ResponseEntity<?> getMonthlySummary(@RequestParam UUID userId, @RequestParam int year, @RequestParam int month) {
        return ResponseEntity.ok(expenseService.getMonthlySummary(userId, year, month));
    }

    @GetMapping("/breakdown")
    public ResponseEntity<?> getCategoryBreakdown(@RequestParam UUID userId, @RequestParam int year, @RequestParam int month) {
        return ResponseEntity.ok(expenseService.getCategoryBreakdown(userId, year, month));
    }
}