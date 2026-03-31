package com.semicolon.expensetracker.dtos.request;

import com.semicolon.expensetracker.data.models.enums.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class AddExpenseRequest {
    @NotNull(message = "walletId is required")
    private UUID walletId;
    @NotNull(message = "category")
    private UUID categoryId;
    @NotNull
    @Positive(message = "Amount must be greater than zero")
    private BigDecimal amount;
    @NotNull
    private LocalDateTime entryDate;
    private String note;
    @NotNull(message = "payment method is required")
    private PaymentMethod paymentMethod;
}
