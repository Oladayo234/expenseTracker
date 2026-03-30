package com.semicolon.expensetracker.dtos.request;

import lombok.Data;

import java.util.UUID;

@Data
public class DeleteCategoryRequest {
    private UUID categoryId;
    private UUID userId;
}
