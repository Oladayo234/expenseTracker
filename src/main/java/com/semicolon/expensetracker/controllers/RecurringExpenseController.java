package com.semicolon.expensetracker.controllers;

import com.semicolon.expensetracker.dtos.request.AddRecurringExpenseRequest;
import com.semicolon.expensetracker.dtos.response.RecurringExpenseResponse;
import com.semicolon.expensetracker.services.RecurringExpenseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/recurring-expenses")
@RequiredArgsConstructor
public class RecurringExpenseController {

    private final RecurringExpenseService recurringExpenseService;

    @PostMapping
    public ResponseEntity<RecurringExpenseResponse> create(@Valid @RequestBody AddRecurringExpenseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(recurringExpenseService.createRecurringExpense(request));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<RecurringExpenseResponse>> getByUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(recurringExpenseService.getRecurringExpensesByUser(userId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable UUID id, @RequestParam UUID userId) {
        recurringExpenseService.deleteRecurringExpense(id, userId);
        return ResponseEntity.ok("Recurring expense deleted successfully");
    }
}