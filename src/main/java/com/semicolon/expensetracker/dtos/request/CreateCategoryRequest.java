package com.semicolon.expensetracker.dtos.request;

import com.semicolon.expensetracker.data.models.enums.TransactionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateCategoryRequest {
    @NotBlank(message = "Category name is required")
    private String name;
    @NotNull(message = "Transaction type is required")
    private TransactionType transactionType;
}