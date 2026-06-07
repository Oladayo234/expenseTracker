package com.semicolon.expensetracker.controllers;

import com.semicolon.expensetracker.dtos.request.AddRecurringExpenseRequest;
import com.semicolon.expensetracker.services.RecurringExpenseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/recurring-expenses")
@RequiredArgsConstructor
public class RecurringExpenseController {

    private final RecurringExpenseService recurringExpenseService;

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody AddRecurringExpenseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(recurringExpenseService.createRecurringExpense(request));
    }

    @GetMapping
    public ResponseEntity<?> getMyRecurringExpenses() {
        return ResponseEntity.ok(recurringExpenseService.getRecurringExpensesByUser());
    }

    @DeleteMapping("/{publicId}")
    public ResponseEntity<?> delete(@PathVariable UUID publicId) {
        recurringExpenseService.deleteRecurringExpense(publicId);
        return ResponseEntity.ok("Recurring expense deleted successfully");
    }
}