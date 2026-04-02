package com.semicolon.expensetracker.dtos.request;

import com.semicolon.expensetracker.data.models.Category;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.UUID;

@Data
public class CreateBudgetRequest {
    @NotNull(message = "Category ID is required")
    private UUID categoryId;
    private String budgetName;

    @NotNull(message = "Budget amount is required")
    @Positive(message = "Budget amount must be positive")
    private BigDecimal budgetAmount;

    @NotNull(message = "User ID is required")
    private UUID userId;

    @NotNull(message = "Year and Month is required")
    private YearMonth yearMonth;
}
