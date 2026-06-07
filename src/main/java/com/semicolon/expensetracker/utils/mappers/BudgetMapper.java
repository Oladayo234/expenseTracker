package com.semicolon.expensetracker.utils.mappers;

import com.semicolon.expensetracker.data.models.Budget;
import com.semicolon.expensetracker.dtos.response.BudgetLimitVsActualExpenseResponse;
import com.semicolon.expensetracker.dtos.response.BudgetResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class BudgetMapper {

    public BudgetResponse toResponse(Budget budget, BigDecimal actualSpent, String message) {
        BudgetResponse response = new BudgetResponse();
        response.setPublicId(budget.getPublicId());
        response.setBudgetName(budget.getBudgetName());
        response.setBudgetAmount(budget.getBudgetAmount());
        response.setCategoryId(budget.getCategory().getPublicId());
        response.setCategoryName(budget.getCategory().getName());
        response.setYearMonth(budget.getMonthYear());
        response.setActualSpent(actualSpent);
        response.setAmountRemaining(budget.getBudgetAmount().subtract(actualSpent));
        response.setMessage(message);
        return response;
    }

    public BudgetLimitVsActualExpenseResponse toVsActualResponse(Budget budget, BigDecimal actualSpent) {
        BigDecimal budgetAmount = budget.getBudgetAmount();
        BigDecimal remaining = budgetAmount.subtract(actualSpent);
        double percentageUsed = budgetAmount.compareTo(BigDecimal.ZERO) == 0
                ? 0.0
                : actualSpent.multiply(BigDecimal.valueOf(100))
                .divide(budgetAmount, 2, RoundingMode.HALF_UP)
                .doubleValue();
        String status = percentageUsed >= 100 ? "EXCEEDED"
                : percentageUsed >= 80  ? "WARNING"
                  : "ON_TRACK";
        return BudgetLimitVsActualExpenseResponse.builder()
                .budgetId(budget.getPublicId())
                .categoryId(budget.getCategory().getPublicId())
                .categoryName(budget.getCategory().getName())
                .budgetAmount(budgetAmount)
                .actualSpent(actualSpent)
                .remaining(remaining)
                .percentageUsed(percentageUsed)
                .status(status)
                .build();
    }
}