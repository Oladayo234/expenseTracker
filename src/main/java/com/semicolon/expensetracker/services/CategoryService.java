package com.semicolon.expensetracker.services;

import com.semicolon.expensetracker.data.models.Category;
import com.semicolon.expensetracker.data.models.User;
import com.semicolon.expensetracker.data.repositories.CategoryRepository;
import com.semicolon.expensetracker.data.repositories.ExpenseRepository;
import com.semicolon.expensetracker.dtos.request.CreateCategoryRequest;
import com.semicolon.expensetracker.dtos.request.DeleteCategoryRequest;
import com.semicolon.expensetracker.dtos.response.CreateCategoryResponse;
import com.semicolon.expensetracker.exceptions.InvalidEntryException;
import com.semicolon.expensetracker.utils.AuthUtils;
import com.semicolon.expensetracker.utils.mappers.CategoryMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ExpenseRepository expenseRepository;
    private final CategoryMapper categoryMapper;

    @Transactional
    public CreateCategoryResponse createCategory(CreateCategoryRequest request) {
        User user = AuthUtils.getCurrentUser();
        if (categoryRepository.existsByNameAndUserId(request.getName(), user.getId())) {
            throw new InvalidEntryException("Category '" + request.getName() + "' already exists");
        }
        Category category = new Category();
        category.setName(request.getName());
        category.setTransactionType(request.getTransactionType());
        category.setDefaultCategory(false);
        category.setUser(user);
        return categoryMapper.toResponse(categoryRepository.save(category));
    }

    public List<CreateCategoryResponse> getCategories() {
        UUID userId = AuthUtils.getCurrentUserId();
        List<CreateCategoryResponse> responses = new ArrayList<>();
        for (Category category : categoryRepository.findByUserIdOrDefaultCategoryTrue(userId)) {
            responses.add(categoryMapper.toResponse(category));
        }
        return responses;
    }

    @Transactional
    public void deleteCategory(DeleteCategoryRequest request) {
        UUID userId = AuthUtils.getCurrentUserId();
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new InvalidEntryException("Category does not exist"));
        if (category.isDefaultCategory()) {
            throw new InvalidEntryException("Cannot delete default categories");
        }
        if (!category.getUser().getId().equals(userId)) {
            throw new InvalidEntryException("You don't have permission to delete this category");
        }
        Category uncategorized = findOrCreateUncategorized();
        expenseRepository.reassignExpensesToCategory(category.getId(), uncategorized.getId());
        categoryRepository.delete(category);
    }

    private Category findOrCreateUncategorized() {
        return categoryRepository.findByNameAndDefaultCategoryTrue("Uncategorized")
                .orElseGet(() -> {
                    Category category = new Category();
                    category.setName("Uncategorized");
                    category.setTransactionType(null);
                    category.setDefaultCategory(true);
                    category.setUser(null);
                    return categoryRepository.save(category);
                });
    }
}