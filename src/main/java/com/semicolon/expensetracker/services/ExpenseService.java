package com.semicolon.expensetracker.services;

import com.semicolon.expensetracker.data.models.Category;
import com.semicolon.expensetracker.data.models.Expense;
import com.semicolon.expensetracker.data.models.Wallet;
import com.semicolon.expensetracker.data.models.enums.TransactionType;
import com.semicolon.expensetracker.data.repositories.CategoryRepository;
import com.semicolon.expensetracker.data.repositories.ExpenseRepository;
import com.semicolon.expensetracker.data.repositories.UserRepository;
import com.semicolon.expensetracker.data.repositories.WalletRepository;
import com.semicolon.expensetracker.dtos.request.AddExpenseRequest;
import com.semicolon.expensetracker.dtos.response.AddExpenseResponse;
import com.semicolon.expensetracker.dtos.response.CategoryBreakdownResponse;
import com.semicolon.expensetracker.dtos.response.MonthlySummaryResponse;
import com.semicolon.expensetracker.exceptions.InvalidEntryException;
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
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final CategoryRepository categoryRepository;

    @Transactional
    public AddExpenseResponse addExpense(AddExpenseRequest request) {
        validateUserExists(request.getUserId());
        Wallet wallet = findWalletById(request.getWalletId());
        Category category = findCategoryById(request.getCategoryId());
        validateWalletOwnership(wallet, request.getUserId());
        validateCategoryAccess(category, request.getUserId());
        Expense expense = new Expense();
        expense.setWallet(wallet);
        expense.setCategory(category);
        expense.setAmount(request.getAmount());
        expense.setNote(request.getNote());
        expense.setPaymentMethod(request.getPaymentMethod());
        expense.setExpenseDate(request.getExpenseDate() != null ? request.getExpenseDate() : LocalDateTime.now());
        return buildAddExpenseResponse(expenseRepository.save(expense));
    }

    @Transactional
    public void deleteExpense(UUID expenseId, UUID userId) {
        Expense expense = findExpenseById(expenseId);
        if (!expense.getWallet().getUser().getId().equals(userId)) {
            throw new InvalidEntryException("Expense does not belong to this user");
        }
        expenseRepository.delete(expense);
    }

    public List<AddExpenseResponse> getExpensesByWallet(UUID walletId, UUID userId) {
        Wallet wallet = findWalletById(walletId);
        validateWalletOwnership(wallet, userId);
        List<Expense> expenses = expenseRepository.findByWalletIdOrderByExpenseDateDesc(walletId);
        List<AddExpenseResponse> responses = new ArrayList<>();
        for (Expense expense : expenses) {
            responses.add(buildAddExpenseResponse(expense));
        }
        return responses;
    }

    public List<AddExpenseResponse> getExpensesByCategory(UUID categoryId, UUID userId) {
        Category category = findCategoryById(categoryId);
        validateCategoryAccess(category, userId);
        List<Expense> expenses = expenseRepository.findByCategoryIdOrderByExpenseDateDesc(categoryId);
        List<AddExpenseResponse> responses = new ArrayList<>();
        for (Expense expense : expenses) {
            responses.add(buildAddExpenseResponse(expense));
        }
        return responses;
    }

    public List<AddExpenseResponse> getExpensesByDateRange(UUID walletId, UUID userId,
                                                           LocalDateTime startDate, LocalDateTime endDate) {
        Wallet wallet = findWalletById(walletId);
        validateWalletOwnership(wallet, userId);
        List<Expense> expenses = expenseRepository.findByWalletIdAndExpenseDateBetweenOrderByExpenseDateDesc(
                walletId, startDate, endDate);
        List<AddExpenseResponse> responses = new ArrayList<>();
        for (Expense expense : expenses) {
            responses.add(buildAddExpenseResponse(expense));
        }
        return responses;
    }

    public MonthlySummaryResponse getMonthlySummary(UUID userId, int year, int month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDateTime startDate = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime endDate = yearMonth.atEndOfMonth().atTime(23, 59, 59);
        validateUserExists(userId);
        List<Expense> expenses = expenseRepository.findByWalletUserIdAndExpenseDateBetween(userId, startDate, endDate);
        BigDecimal totalInflow = BigDecimal.ZERO;
        BigDecimal totalFixedOutflow = BigDecimal.ZERO;
        BigDecimal totalVariableOutflow = BigDecimal.ZERO;
        for (Expense expense : expenses) {
            Category category = expense.getCategory();
            if (category.getTransactionType() == TransactionType.INFLOW) {
                totalInflow = totalInflow.add(expense.getAmount());
            } else if (category.getTransactionType() == TransactionType.OUTFLOW_FIXED_COST) {
                totalFixedOutflow = totalFixedOutflow.add(expense.getAmount());
            } else if (category.getTransactionType() == TransactionType.OUTFLOW_VARIABLE_COST) {
                totalVariableOutflow = totalVariableOutflow.add(expense.getAmount());
            }
        }
        BigDecimal totalOutflow = totalFixedOutflow.add(totalVariableOutflow);
        BigDecimal netIncome = totalInflow.subtract(totalOutflow);
        return MonthlySummaryResponse.builder()
                .year(year)
                .month(month)
                .totalIncome(totalInflow)
                .totalExpenses(totalFixedOutflow.add(totalVariableOutflow))
                .netAmount(netIncome)
                .build();
    }

    public List<CategoryBreakdownResponse> getCategoryBreakdown(UUID userId, int year, int month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDateTime startDate = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime endDate = yearMonth.atEndOfMonth().atTime(23, 59, 59);
        validateUserExists(userId);
        List<Object[]> results = expenseRepository.findCategoryBreakdownByUserIdAndDateRange(userId, startDate, endDate);
        List<CategoryBreakdownResponse> breakdown = new ArrayList<>();
        for (Object[] result : results) {
            CategoryBreakdownResponse response = CategoryBreakdownResponse.builder()
                    .categoryId((UUID) result[0])
                    .categoryName((String) result[1])
                    .totalAmount((BigDecimal) result[3])
                    .build();
            breakdown.add(response);
        }
        return breakdown;
    }

    private void validateWalletOwnership(Wallet wallet, UUID userId) {
        if (!wallet.getUser().getId().equals(userId)) {
            throw new InvalidEntryException("Wallet does not belong to this user");
        }
    }

    private void validateCategoryAccess(Category category, UUID userId) {
        if (!category.isDefault() && category.getUser() != null && !category.getUser().getId().equals(userId)) {
            throw new InvalidEntryException("Category does not belong to this user");
        }
    }

    private void validateUserExists(UUID userId) {
        if (!userRepository.existsById(userId)) {
            throw new InvalidEntryException("User does not exist");
        }
    }

    private Wallet findWalletById(UUID walletId) {
        return walletRepository.findById(walletId)
                .orElseThrow(() -> new InvalidEntryException("Wallet does not exist"));
    }

    private Category findCategoryById(UUID categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new InvalidEntryException("Category does not exist"));
    }

    private Expense findExpenseById(UUID expenseId) {
        return expenseRepository.findById(expenseId)
                .orElseThrow(() -> new InvalidEntryException("Expense does not exist"));
    }

    private AddExpenseResponse buildAddExpenseResponse(Expense expense) {
        AddExpenseResponse response = new AddExpenseResponse();
        response.setId(expense.getId());
        response.setWalletId(expense.getWallet().getId());
        response.setCategoryId(expense.getCategory().getId());
        response.setCategoryName(expense.getCategory().getName());
        response.setAmount(expense.getAmount());
        response.setNote(expense.getNote());
        response.setPaymentMethod(expense.getPaymentMethod());
        response.setExpenseDate(expense.getExpenseDate());
        response.setMessage("Expense added successfully");
        return response;
    }
}