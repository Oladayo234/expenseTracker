package com.semicolon.expensetracker.controllers;

import com.semicolon.expensetracker.dtos.request.CreateBudgetRequest;
import com.semicolon.expensetracker.dtos.response.BudgetLimitVsActualExpenseResponse;
import com.semicolon.expensetracker.dtos.response.BudgetResponse;
import com.semicolon.expensetracker.services.BudgetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/budgets")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgetService;

    @PostMapping
    public ResponseEntity<BudgetResponse> createBudget(@Valid @RequestBody CreateBudgetRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(budgetService.createBudget(request));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<BudgetResponse>> getBudgetsByUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(budgetService.getBudgetsByUser(userId));
    }

    @GetMapping("/user/{userId}/vs-actual")
    public ResponseEntity<List<BudgetLimitVsActualExpenseResponse>> getBudgetVsActual(@PathVariable UUID userId) {
        return ResponseEntity.ok(budgetService.getBudgetVsActual(userId));
    }

    @DeleteMapping("/{budgetId}")
    public ResponseEntity<String> deleteBudget(@PathVariable UUID budgetId, @RequestParam UUID userId) {
        budgetService.deleteBudget(budgetId, userId);
        return ResponseEntity.ok("Budget deleted successfully");
    }
}