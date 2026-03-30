package com.semicolon.expensetracker.dtos.request;

import com.semicolon.expensetracker.data.models.enums.TransactionType;

import java.util.UUID;

public class UpdateCategoryRequest {
    private UUID categoryId;
    private String name;
    private TransactionType transactionType;
}
