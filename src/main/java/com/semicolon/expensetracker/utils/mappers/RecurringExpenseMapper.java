package com.semicolon.expensetracker.utils.mappers;

import com.semicolon.expensetracker.data.models.RecurringExpenses;
import com.semicolon.expensetracker.dtos.response.RecurringExpenseResponse;
import org.springframework.stereotype.Component;

@Component
public class RecurringExpenseMapper {

    public RecurringExpenseResponse toResponse(RecurringExpenses expense) {
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