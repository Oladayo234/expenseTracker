package com.semicolon.expensetracker.controllers;

import com.semicolon.expensetracker.dtos.request.AddExpenseRequest;
import com.semicolon.expensetracker.services.ExpenseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
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
    public ResponseEntity<?> getExpensesByWallet(@PathVariable UUID walletId) {
        return ResponseEntity.ok(expenseService.getExpensesByWallet(walletId));
    }

    @GetMapping("/wallet/{walletId}/range")
    public ResponseEntity<?> getExpensesByDateRange(
            @PathVariable UUID walletId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return ResponseEntity.ok(expenseService.getExpensesByDateRange(walletId, start, end));
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<?> getExpensesByCategory(@PathVariable UUID categoryId) {
        return ResponseEntity.ok(expenseService.getExpensesByCategory(categoryId));
    }

    @DeleteMapping("/{expenseId}")
    public ResponseEntity<?> deleteExpense(@PathVariable UUID expenseId) {
        expenseService.deleteExpense(expenseId);
        return ResponseEntity.ok("Expense deleted successfully");
    }

    @GetMapping("/summary")
    public ResponseEntity<?> getMonthlySummary(
            @RequestParam int year,
            @RequestParam int month) {
        return ResponseEntity.ok(expenseService.getMonthlySummary(year, month));
    }

    @GetMapping("/breakdown")
    public ResponseEntity<?> getCategoryBreakdown(
            @RequestParam int year,
            @RequestParam int month) {
        return ResponseEntity.ok(expenseService.getCategoryBreakdown(year, month));
    }
}