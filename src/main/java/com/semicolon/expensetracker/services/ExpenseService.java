package com.semicolon.expensetracker.services;

import com.semicolon.expensetracker.data.models.Category;
import com.semicolon.expensetracker.data.models.Expense;
import com.semicolon.expensetracker.data.models.Wallet;
import com.semicolon.expensetracker.data.models.enums.TransactionType;
import com.semicolon.expensetracker.data.repositories.CategoryRepository;
import com.semicolon.expensetracker.data.repositories.ExpenseRepository;
import com.semicolon.expensetracker.data.repositories.WalletRepository;
import com.semicolon.expensetracker.dtos.request.AddExpenseRequest;
import com.semicolon.expensetracker.dtos.response.AddExpenseResponse;
import com.semicolon.expensetracker.dtos.response.CategoryBreakdownResponse;
import com.semicolon.expensetracker.dtos.response.MonthlySummaryResponse;
import com.semicolon.expensetracker.exceptions.InvalidEntryException;
import com.semicolon.expensetracker.utils.AuthUtils;
import com.semicolon.expensetracker.utils.mappers.ExpenseMapper;
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
    private final WalletRepository walletRepository;
    private final CategoryRepository categoryRepository;
    private final ExpenseMapper expenseMapper;

    @Transactional
    public AddExpenseResponse addExpense(AddExpenseRequest request) {
        UUID userId = AuthUtils.getCurrentUserId();
        Wallet wallet = findWalletById(request.getWalletId());
        validateWalletOwnership(wallet, userId);
        Category category = findCategoryById(request.getCategoryId());
        validateCategoryAccess(category, userId);
        Expense expense = new Expense();
        expense.setWallet(wallet);
        expense.setCategory(category);
        expense.setAmount(request.getAmount());
        expense.setNote(request.getNote());
        expense.setPaymentMethod(request.getPaymentMethod());
        expense.setExpenseDate(request.getExpenseDate() != null
                ? request.getExpenseDate() : LocalDateTime.now());
        return expenseMapper.toResponse(expenseRepository.save(expense));
    }

    @Transactional
    public void deleteExpense(UUID expenseId) {
        UUID userId = AuthUtils.getCurrentUserId();
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new InvalidEntryException("Expense does not exist"));
        if (!expense.getWallet().getUser().getId().equals(userId)) {
            throw new InvalidEntryException("Expense does not belong to this user");
        }
        expenseRepository.delete(expense);
    }

    public List<AddExpenseResponse> getExpensesByWallet(UUID walletId) {
        UUID userId = AuthUtils.getCurrentUserId();
        Wallet wallet = findWalletById(walletId);
        validateWalletOwnership(wallet, userId);
        List<AddExpenseResponse> responses = new ArrayList<>();
        for (Expense e : expenseRepository.findByWalletIdOrderByExpenseDateDesc(walletId)) {
            responses.add(expenseMapper.toResponse(e));
        }
        return responses;
    }

    public List<AddExpenseResponse> getExpensesByCategory(UUID categoryId) {
        UUID userId = AuthUtils.getCurrentUserId();
        Category category = findCategoryById(categoryId);
        validateCategoryAccess(category, userId);
        List<AddExpenseResponse> responses = new ArrayList<>();
        for (Expense e : expenseRepository.findByCategoryIdOrderByExpenseDateDesc(categoryId)) {
            responses.add(expenseMapper.toResponse(e));
        }
        return responses;
    }

    public List<AddExpenseResponse> getExpensesByDateRange(UUID walletId,
                                                           LocalDateTime start,
                                                           LocalDateTime end) {
        UUID userId = AuthUtils.getCurrentUserId();
        Wallet wallet = findWalletById(walletId);
        validateWalletOwnership(wallet, userId);
        List<AddExpenseResponse> responses = new ArrayList<>();
        for (Expense e : expenseRepository
                .findByWalletIdAndExpenseDateBetweenOrderByExpenseDateDesc(walletId, start, end)) {
            responses.add(expenseMapper.toResponse(e));
        }
        return responses;
    }

    public MonthlySummaryResponse getMonthlySummary(int year, int month) {
        UUID userId = AuthUtils.getCurrentUserId();
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDateTime start = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime end = yearMonth.atEndOfMonth().atTime(23, 59, 59);
        List<Expense> expenses = expenseRepository
                .findByWalletUserIdAndExpenseDateBetween(userId, start, end);
        BigDecimal totalInflow = BigDecimal.ZERO;
        BigDecimal totalFixedOutflow = BigDecimal.ZERO;
        BigDecimal totalVariableOutflow = BigDecimal.ZERO;
        for (Expense expense : expenses) {
            TransactionType type = expense.getCategory().getTransactionType();
            if (type == null) continue;
            switch (type) {
                case INFLOW -> totalInflow = totalInflow.add(expense.getAmount());
                case OUTFLOW_FIXED_COST ->
                        totalFixedOutflow = totalFixedOutflow.add(expense.getAmount());
                case OUTFLOW, OUTFLOW_VARIABLE_COST ->
                        totalVariableOutflow = totalVariableOutflow.add(expense.getAmount());
            }
        }
        BigDecimal totalOutflow = totalFixedOutflow.add(totalVariableOutflow);
        return MonthlySummaryResponse.builder()
                .year(year).month(month)
                .totalIncome(totalInflow)
                .totalExpenses(totalOutflow)
                .netAmount(totalInflow.subtract(totalOutflow))
                .build();
    }

    public List<CategoryBreakdownResponse> getCategoryBreakdown(int year, int month) {
        UUID userId = AuthUtils.getCurrentUserId();
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDateTime start = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime end = yearMonth.atEndOfMonth().atTime(23, 59, 59);
        List<Object[]> results = expenseRepository
                .findCategoryBreakdownByUserIdAndDateRange(userId, start, end);
        List<CategoryBreakdownResponse> breakdown = new ArrayList<>();
        for (Object[] result : results) {
            breakdown.add(CategoryBreakdownResponse.builder()
                    .categoryId((UUID) result[0])
                    .categoryName((String) result[1])
                    .totalAmount((BigDecimal) result[3])
                    .build());
        }
        return breakdown;
    }

    private void validateWalletOwnership(Wallet wallet, UUID userId) {
        if (!wallet.getUser().getId().equals(userId)) {
            throw new InvalidEntryException("Wallet does not belong to this user");
        }
    }

    private void validateCategoryAccess(Category category, UUID userId) {
        if (!category.isDefaultCategory() && category.getUser() != null
                && !category.getUser().getId().equals(userId)) {
            throw new InvalidEntryException("Category does not belong to this user");
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
}