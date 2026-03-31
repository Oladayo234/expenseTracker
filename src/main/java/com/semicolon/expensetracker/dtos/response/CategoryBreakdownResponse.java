package com.semicolon.expensetracker.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryBreakdownResponse {
    private UUID categoryId;
    private String categoryName;
    private BigDecimal totalAmount;
    private double percentage;
}