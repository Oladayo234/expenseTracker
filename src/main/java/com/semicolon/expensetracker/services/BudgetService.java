package com.semicolon.expensetracker.services;

import com.semicolon.expensetracker.data.models.Budget;
import com.semicolon.expensetracker.data.models.Category;
import com.semicolon.expensetracker.data.models.User;
import com.semicolon.expensetracker.data.repositories.BudgetRepository;
import com.semicolon.expensetracker.data.repositories.CategoryRepository;
import com.semicolon.expensetracker.data.repositories.ExpenseRepository;
import com.semicolon.expensetracker.dtos.request.CreateBudgetRequest;
import com.semicolon.expensetracker.dtos.response.BudgetLimitVsActualExpenseResponse;
import com.semicolon.expensetracker.dtos.response.BudgetResponse;
import com.semicolon.expensetracker.exceptions.ForbiddenException;
import com.semicolon.expensetracker.exceptions.InvalidEntryException;
import com.semicolon.expensetracker.exceptions.ResourceNotFoundException;
import com.semicolon.expensetracker.utils.AuthUtils;
import com.semicolon.expensetracker.utils.mappers.BudgetMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final CategoryRepository categoryRepository;
    private final ExpenseRepository expenseRepository;
    private final BudgetMapper budgetMapper;

    @Transactional
    public BudgetResponse createBudget(CreateBudgetRequest request) {
        User user = AuthUtils.getCurrentUser();
        Category category = categoryRepository.findByPublicId(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category does not exist"));
        if (budgetRepository.existsByUserIdAndCategoryIdAndMonthYear(
                user.getId(), category.getId(), request.getYearMonth())) {
            throw new InvalidEntryException("Budget already exists for this category and month");
        }
        Budget budget = new Budget();
        budget.setBudgetName(request.getBudgetName());
        budget.setBudgetAmount(request.getBudgetAmount());
        budget.setMonthYear(request.getYearMonth());
        budget.setCategory(category);
        budget.setUser(user);
        Budget saved = budgetRepository.save(budget);
        return budgetMapper.toResponse(saved, computeActualSpent(saved), "Budget created successfully");
    }

    public List<BudgetResponse> getBudgetsByUser() {
        Long userId = AuthUtils.getCurrentUserId();
        List<BudgetResponse> responses = new ArrayList<>();
        for (Budget b : budgetRepository.findByUserId(userId)) {
            responses.add(budgetMapper.toResponse(b, computeActualSpent(b), "Success"));
        }
        return responses;
    }

    public List<BudgetLimitVsActualExpenseResponse> getBudgetVsActual() {
        Long userId = AuthUtils.getCurrentUserId();
        List<BudgetLimitVsActualExpenseResponse> responses = new ArrayList<>();
        for (Budget b : budgetRepository.findByUserId(userId)) {
            responses.add(budgetMapper.toVsActualResponse(b, computeActualSpent(b)));
        }
        return responses;
    }

    @Transactional
    public void deleteBudget(UUID publicId) {
        Long userId = AuthUtils.getCurrentUserId();
        Budget budget = budgetRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget does not exist"));
        if (!budget.getUser().getId().equals(userId)) {
            throw new ForbiddenException("Budget does not belong to this user");
        }
        budgetRepository.delete(budget);
    }
    private BigDecimal computeActualSpent(Budget budget) {
        YearMonth yearMonth = budget.getMonthYear();
        LocalDateTime start = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime end = yearMonth.atEndOfMonth().atTime(23, 59, 59);
        return expenseRepository.sumByCategoryAndUserAndDateRange(
                        budget.getUser().getId(), budget.getCategory().getId(), start, end)
                .orElse(BigDecimal.ZERO);
    }
}