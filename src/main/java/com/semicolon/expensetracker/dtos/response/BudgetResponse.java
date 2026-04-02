package com.semicolon.expensetracker.dtos.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.UUID;

@Data
public class BudgetResponse {
    private UUID id;
    private String categoryName;
    private UUID categoryId;
    private BigDecimal actualSpent;
    private BigDecimal amountRemaining;
    private String budgetName;
    private BigDecimal budgetAmount;
    private YearMonth yearMonth;
    private String message;

}
