package com.semicolon.expensetracker.utils.mappers;

import com.semicolon.expensetracker.data.models.Expense;
import com.semicolon.expensetracker.dtos.response.AddExpenseResponse;
import org.springframework.stereotype.Component;

@Component
public class ExpenseMapper {

    public AddExpenseResponse toResponse(Expense expense) {
        AddExpenseResponse response = new AddExpenseResponse();
        response.setPublicId(expense.getPublicId());
        response.setWalletId(expense.getWallet().getPublicId());
        response.setCategoryId(expense.getCategory().getPublicId());
        response.setCategoryName(expense.getCategory().getName());
        response.setAmount(expense.getAmount());
        response.setNote(expense.getNote());
        response.setPaymentMethod(expense.getPaymentMethod());
        response.setExpenseDate(expense.getExpenseDate());
        response.setMessage("Expense added successfully");
        return response;
    }
}