package com.semicolon.expensetracker.dtos.response;

import com.semicolon.expensetracker.data.models.enums.TransactionType;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Builder
@Data
public class CreateCategoryResponse {
    private String name;
    private UUID id;
    private TransactionType transactionType;
    private boolean isDefault;
    private String icon;
    private UUID userId;
}
