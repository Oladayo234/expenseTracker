package com.semicolon.expensetracker.services;

import com.semicolon.expensetracker.data.models.Category;
import com.semicolon.expensetracker.data.models.RecurringExpenses;
import com.semicolon.expensetracker.data.models.User;
import com.semicolon.expensetracker.data.models.Wallet;
import com.semicolon.expensetracker.data.repositories.CategoryRepository;
import com.semicolon.expensetracker.data.repositories.RecurringExpensesRepository;
import com.semicolon.expensetracker.data.repositories.WalletRepository;
import com.semicolon.expensetracker.dtos.request.AddRecurringExpenseRequest;
import com.semicolon.expensetracker.dtos.response.RecurringExpenseResponse;
import com.semicolon.expensetracker.exceptions.ForbiddenException;
import com.semicolon.expensetracker.exceptions.ResourceNotFoundException;
import com.semicolon.expensetracker.utils.AuthUtils;
import com.semicolon.expensetracker.utils.mappers.RecurringExpenseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RecurringExpenseService {

    private final RecurringExpensesRepository recurringExpensesRepository;
    private final WalletRepository walletRepository;
    private final CategoryRepository categoryRepository;
    private final RecurringExpenseMapper recurringExpenseMapper;

    @Transactional
    public RecurringExpenseResponse createRecurringExpense(AddRecurringExpenseRequest request) {
        User user = AuthUtils.getCurrentUser();
        Wallet wallet = walletRepository.findByPublicId(request.getWalletId())
                .orElseThrow(() -> new ResourceNotFoundException("Wallet does not exist"));
        if (!wallet.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("Wallet does not belong to this user");
        }
        Category category = categoryRepository.findByPublicId(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category does not exist"));
        RecurringExpenses recurring = new RecurringExpenses();
        recurring.setAmount(request.getAmount());
        recurring.setFrequency(request.getFrequency());
        recurring.setNextDueDate(request.getNextDueDate() != null
                ? request.getNextDueDate() : LocalDate.now());
        recurring.setWallet(wallet);
        recurring.setCategory(category);
        recurring.setUser(user);
        return recurringExpenseMapper.toResponse(recurringExpensesRepository.save(recurring));
    }

    public List<RecurringExpenseResponse> getRecurringExpensesByUser() {
        Long userId = AuthUtils.getCurrentUserId();
        List<RecurringExpenseResponse> responses = new ArrayList<>();
        for (RecurringExpenses r : recurringExpensesRepository.findByUserId(userId)) {
            responses.add(recurringExpenseMapper.toResponse(r));
        }
        return responses;
    }

    @Transactional
    public void deleteRecurringExpense(UUID publicId) {
        Long userId = AuthUtils.getCurrentUserId();
        RecurringExpenses expense = recurringExpensesRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Recurring expense does not exist"));
        if (!expense.getUser().getId().equals(userId)) {
            throw new ForbiddenException("Recurring expense does not belong to this user");
        }
        recurringExpensesRepository.delete(expense);
    }
}