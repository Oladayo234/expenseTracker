package com.semicolon.expensetracker.services;

import com.semicolon.expensetracker.data.models.Budget;
import com.semicolon.expensetracker.data.models.Category;
import com.semicolon.expensetracker.data.models.User;
import com.semicolon.expensetracker.data.repositories.BudgetRepository;
import com.semicolon.expensetracker.data.repositories.CategoryRepository;
import com.semicolon.expensetracker.data.repositories.ExpenseRepository;
import com.semicolon.expensetracker.data.repositories.UserRepository;
import com.semicolon.expensetracker.dtos.request.CreateBudgetRequest;
import com.semicolon.expensetracker.dtos.response.BudgetLimitVsActualExpenseResponse;
import com.semicolon.expensetracker.dtos.response.BudgetResponse;
import com.semicolon.expensetracker.exceptions.InvalidEntryException;
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
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ExpenseRepository expenseRepository;
    private final BudgetMapper budgetMapper;

    @Transactional
    public BudgetResponse createBudget(CreateBudgetRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new InvalidEntryException("User does not exist"));
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new InvalidEntryException("Category does not exist"));
        if (budgetRepository.existsByUserIdAndCategoryIdAndMonthYear(
                request.getUserId(), request.getCategoryId(), request.getYearMonth())) {
            throw new InvalidEntryException("Budget already exists for this category and month");
        }
        Budget budget = new Budget();
        budget.setBudgetName(request.getBudgetName());
        budget.setBudgetAmount(request.getBudgetAmount());
        budget.setMonthYear(request.getYearMonth());
        budget.setCategory(category);
        budget.setUser(user);
        Budget saved = budgetRepository.save(budget);
        BigDecimal actualSpent = computeActualSpent(saved);
        return budgetMapper.toResponse(saved, actualSpent, "Budget created successfully");
    }

    public List<BudgetResponse> getBudgetsByUser(UUID userId) {
        if (!userRepository.existsById(userId)) {
            throw new InvalidEntryException("User does not exist");
        }
        List<Budget> budgets = budgetRepository.findByUserId(userId);
        List<BudgetResponse> responses = new ArrayList<>();
        for (Budget budget : budgets) {
            responses.add(budgetMapper.toResponse(budget, computeActualSpent(budget), "Success"));
        }
        return responses;
    }

    public List<BudgetLimitVsActualExpenseResponse> getBudgetVsActual(UUID userId) {
        if (!userRepository.existsById(userId)) {
            throw new InvalidEntryException("User does not exist");
        }
        List<Budget> budgets = budgetRepository.findByUserId(userId);
        List<BudgetLimitVsActualExpenseResponse> responses = new ArrayList<>();
        for (Budget budget : budgets) {
            responses.add(budgetMapper.toVsActualResponse(budget, computeActualSpent(budget)));
        }
        return responses;
    }

    @Transactional
    public void deleteBudget(UUID budgetId, UUID userId) {
        Budget budget = budgetRepository.findByIdAndUserId(budgetId, userId)
                .orElseThrow(() -> new InvalidEntryException("Budget not found or does not belong to this user"));
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