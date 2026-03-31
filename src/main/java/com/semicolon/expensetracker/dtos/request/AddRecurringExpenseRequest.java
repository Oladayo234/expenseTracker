package com.semicolon.expensetracker.dtos.request;

import com.semicolon.expensetracker.data.models.enums.Frequency;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class AddRecurringExpenseRequest {
    @NotNull(message = "userId is required")
    private UUID userId;

    @NotNull(message = "walletId is required")
    private UUID walletId;

    @NotNull(message = "categoryId is required")
    private UUID categoryId;

    @NotNull
    @Positive(message = "Amount must be greater than zero")
    private BigDecimal amount;

    @NotNull(message = "frequency is required")
    private Frequency frequency;

    private LocalDate nextDueDate;
}