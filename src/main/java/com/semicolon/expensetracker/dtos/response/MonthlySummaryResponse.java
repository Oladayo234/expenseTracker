package com.semicolon.expensetracker.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlySummaryResponse {
    private int year;
    private int month;
    private BigDecimal totalExpenses;
    private BigDecimal totalIncome;
    private BigDecimal netAmount;
    private String message;
}