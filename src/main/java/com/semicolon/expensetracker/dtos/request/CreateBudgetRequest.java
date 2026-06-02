package com.semicolon.expensetracker.dtos.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.UUID;

@Getter
@Setter
public class CreateBudgetRequest {
    @NotNull(message = "Category ID is required")
    private UUID categoryId;
    private String budgetName;
    @NotNull(message = "Budget amount is required")
    @Positive(message = "Budget amount must be positive")
    private BigDecimal budgetAmount;
    @NotNull(message = "Year and Month is required")
    private YearMonth yearMonth;
}