package com.semicolon.expensetracker.services;

import com.semicolon.expensetracker.data.models.Category;
import com.semicolon.expensetracker.data.models.RecurringExpenses;
import com.semicolon.expensetracker.data.models.User;
import com.semicolon.expensetracker.data.models.Wallet;
import com.semicolon.expensetracker.data.repositories.CategoryRepository;
import com.semicolon.expensetracker.data.repositories.RecurringExpensesRepository;
import com.semicolon.expensetracker.data.repositories.UserRepository;
import com.semicolon.expensetracker.data.repositories.WalletRepository;
import com.semicolon.expensetracker.dtos.request.AddRecurringExpenseRequest;
import com.semicolon.expensetracker.dtos.response.RecurringExpenseResponse;
import com.semicolon.expensetracker.exceptions.InvalidEntryException;
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
    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final CategoryRepository categoryRepository;

    @Transactional
    public RecurringExpenseResponse createRecurringExpense(AddRecurringExpenseRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new InvalidEntryException("User does not exist"));
        Wallet wallet = walletRepository.findById(request.getWalletId())
                .orElseThrow(() -> new InvalidEntryException("Wallet does not exist"));
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new InvalidEntryException("Category does not exist"));
        if (!wallet.getUser().getId().equals(user.getId())) {
            throw new InvalidEntryException("Wallet does not belong to this user");
        }
        RecurringExpenses recurringExpense = new RecurringExpenses();
        recurringExpense.setAmount(request.getAmount());
        recurringExpense.setFrequency(request.getFrequency());
        recurringExpense.setNextDueDate(request.getNextDueDate() != null ? request.getNextDueDate() : LocalDate.now());
        recurringExpense.setWallet(wallet);
        recurringExpense.setCategory(category);
        recurringExpense.setUser(user);
        return toResponse(recurringExpensesRepository.save(recurringExpense));
    }

    public List<RecurringExpenseResponse> getRecurringExpensesByUser(UUID userId) {
        if (!userRepository.existsById(userId)) {
            throw new InvalidEntryException("User does not exist");
        }
        List<RecurringExpenses> expenses = recurringExpensesRepository.findByUserId(userId);
        List<RecurringExpenseResponse> responses = new ArrayList<>();
        for (RecurringExpenses expense : expenses) {
            responses.add(toResponse(expense));
        }
        return responses;
    }

    @Transactional
    public void deleteRecurringExpense(UUID recurringExpenseId, UUID userId) {
        RecurringExpenses expense = recurringExpensesRepository.findById(recurringExpenseId)
                .orElseThrow(() -> new InvalidEntryException("Recurring expense does not exist"));
        if (!expense.getUser().getId().equals(userId)) {
            throw new InvalidEntryException("Recurring expense does not belong to this user");
        }
        recurringExpensesRepository.delete(expense);
    }

    private RecurringExpenseResponse toResponse(RecurringExpenses expense) {
        RecurringExpenseResponse response = new RecurringExpenseResponse();
        response.setId(expense.getId());
        response.setAmount(expense.getAmount());
        response.setFrequency(expense.getFrequency());
        response.setNextDueDate(expense.getNextDueDate());
        response.setWalletId(expense.getWallet().getId());
        response.setCategoryId(expense.getCategory().getId());
        response.setCategoryName(expense.getCategory().getName());
        response.setMessage("Success");
        return response;
    }
}