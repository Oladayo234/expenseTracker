package com.semicolon.expensetracker.controllers;

import com.semicolon.expensetracker.dtos.request.CreateBudgetRequest;
import com.semicolon.expensetracker.services.BudgetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/budgets")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgetService;

    @PostMapping
    public ResponseEntity<?> createBudget(@Valid @RequestBody CreateBudgetRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(budgetService.createBudget(request));
    }

    @GetMapping
    public ResponseEntity<?> getMyBudgets() {
        return ResponseEntity.ok(budgetService.getBudgetsByUser());
    }

    @GetMapping("/vs-actual")
    public ResponseEntity<?> getBudgetVsActual() {
        return ResponseEntity.ok(budgetService.getBudgetVsActual());
    }

    @DeleteMapping("/{publicId}")
    public ResponseEntity<?> deleteBudget(@PathVariable UUID publicId) {
        budgetService.deleteBudget(publicId);
        return ResponseEntity.ok("Budget deleted successfully");
    }
}