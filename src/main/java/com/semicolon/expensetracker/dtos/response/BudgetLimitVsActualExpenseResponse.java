package com.semicolon.expensetracker.dtos.response;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class BudgetLimitVsActualExpenseResponse {
    private UUID budgetId;
    private UUID categoryId;
    private String categoryName;
    private BigDecimal budgetAmount;
    private BigDecimal actualSpent;
    private BigDecimal remaining;
    private Double percentageUsed;
    private String status;
}
