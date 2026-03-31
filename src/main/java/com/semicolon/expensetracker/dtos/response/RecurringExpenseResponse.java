package com.semicolon.expensetracker.dtos.response;

import com.semicolon.expensetracker.data.models.enums.Frequency;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecurringExpenseResponse {
    private UUID id;
    private BigDecimal amount;
    private Frequency frequency;
    private LocalDate nextDueDate;
    private UUID walletId;
    private UUID categoryId;
    private String categoryName;
    private String message;
}