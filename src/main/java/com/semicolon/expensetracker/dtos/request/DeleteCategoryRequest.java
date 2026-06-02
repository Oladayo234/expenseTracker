package com.semicolon.expensetracker.dtos.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class DeleteCategoryRequest {
    @NotNull(message = "categoryId is required")
    private UUID categoryId;
}