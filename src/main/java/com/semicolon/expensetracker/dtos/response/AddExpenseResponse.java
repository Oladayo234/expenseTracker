package com.semicolon.expensetracker.dtos.response;

import com.semicolon.expensetracker.data.models.Category;
import com.semicolon.expensetracker.data.models.enums.PaymentMethod;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class AddExpenseResponse {
    private UUID id;
    private UUID walletId;
    private UUID categoryId;
    private String categoryName;
    private BigDecimal amount;
    private String note;
    private PaymentMethod paymentMethod;
    private LocalDateTime expenseDate;
    private String message;

}
