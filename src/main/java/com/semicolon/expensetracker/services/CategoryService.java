package com.semicolon.expensetracker.services;

import com.semicolon.expensetracker.data.models.Category;
import com.semicolon.expensetracker.data.models.User;
import com.semicolon.expensetracker.data.repositories.CategoryRepository;
import com.semicolon.expensetracker.data.repositories.ExpenseRepository;
import com.semicolon.expensetracker.data.repositories.UserRepository;
import com.semicolon.expensetracker.dtos.request.CreateCategoryRequest;
import com.semicolon.expensetracker.dtos.request.DeleteCategoryRequest;
import com.semicolon.expensetracker.dtos.response.CreateCategoryResponse;
import com.semicolon.expensetracker.exceptions.InvalidEntryException;
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
    private final UserRepository userRepository;
    private final ExpenseRepository expenseRepository;

    @Transactional
    public CreateCategoryResponse createCategory(CreateCategoryRequest request) {
        validateCategoryNameNotExists(request.getName(), request.getUserId());

        User user = null;
        if (request.getUserId() != null) {
            user = findUserById(request.getUserId());
        }

        Category category = createCategoryEntity(request, user);
        Category savedCategory = saveCategory(category);

        return mapToCreateCategoryResponse(savedCategory);
    }

    public List<CreateCategoryResponse> getCategories(UUID userId) {
        List<Category> categories;

        if (userId != null) {
            categories = categoryRepository.findByUserIdOrIsDefaultTrue(userId);
        } else {
            categories = categoryRepository.findByIsDefaultTrue();
        }

        List<CreateCategoryResponse> responses = new ArrayList<>();
        for (Category category : categories) {
            responses.add(mapToCreateCategoryResponse(category));
        }

        return responses;
    }

    @Transactional
    public void deleteCategory(DeleteCategoryRequest request) {
        Category categoryToDelete = findCategoryById(request.getCategoryId());

        validateCategoryOwnership(categoryToDelete, request.getUserId());
        validateNotDefaultCategory(categoryToDelete);

        Category uncategorizedCategory = findOrCreateUncategorizedCategory();

        expenseRepository.reassignExpensesToCategory(
                categoryToDelete.getId(),
                uncategorizedCategory.getId()
        );

        categoryRepository.delete(categoryToDelete);
    }

    private Category findOrCreateUncategorizedCategory() {
        return categoryRepository.findByNameAndIsDefaultTrue("Uncategorized")
                .orElseGet(() -> {
                    Category uncategorized = new Category();
                    uncategorized.setName("Uncategorized");
                    uncategorized.setTransactionType(null);
                    uncategorized.setDefault(true);
                    uncategorized.setUser(null);
                    return categoryRepository.save(uncategorized);
                });
    }

    private void validateCategoryNameNotExists(String name, UUID userId) {
        if (userId != null) {
            if (categoryRepository.existsByNameAndUserId(name, userId)) {
                throw new InvalidEntryException("Category with name '" + name + "' already exists for this user");
            }
        } else {
            if (categoryRepository.existsByNameAndIsDefaultTrue(name)) {
                throw new InvalidEntryException("Default category with name '" + name + "' already exists");
            }
        }
    }

    private void validateCategoryOwnership(Category category, UUID userId) {
        if (!category.isDefault() && !category.getUser().getId().equals(userId)) {
            throw new InvalidEntryException("You don't have permission to delete this category");
        }
    }

    private void validateNotDefaultCategory(Category category) {
        if (category.isDefault()) {
            throw new InvalidEntryException("Cannot delete default categories");
        }
    }

    private User findUserById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new InvalidEntryException("User does not exist"));
    }

    private Category findCategoryById(UUID categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new InvalidEntryException("Category does not exist"));
    }

    private Category createCategoryEntity(CreateCategoryRequest request, User user) {
        Category category = new Category();
        category.setName(request.getName());
        category.setTransactionType(request.getTransactionType());
        category.setDefault(request.getUserId() == null);
        category.setUser(user);
        return category;
    }

    private Category saveCategory(Category category) {
        return categoryRepository.save(category);
    }

    private CreateCategoryResponse mapToCreateCategoryResponse(Category category) {
        return CreateCategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .transactionType(category.getTransactionType())
                .isDefault(category.isDefault())
                .userId(category.getUser() != null ? category.getUser().getId() : null)
                .build();
    }
}