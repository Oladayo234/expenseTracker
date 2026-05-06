package com.semicolon.expensetracker.utils.mappers;

import com.semicolon.expensetracker.data.models.Category;
import com.semicolon.expensetracker.dtos.response.CreateCategoryResponse;
import org.springframework.stereotype.Component;

@Component
public class CategoryMapper {

    public CreateCategoryResponse toResponse(Category category) {
        return CreateCategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .transactionType(category.getTransactionType())
                .isDefault(category.isDefaultCategory())
                .build();
    }
}