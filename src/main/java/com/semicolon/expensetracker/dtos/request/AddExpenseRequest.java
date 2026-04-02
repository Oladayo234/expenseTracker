package com.semicolon.expensetracker.dtos.request;

import com.semicolon.expensetracker.data.models.enums.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddExpenseRequest {
    @NotNull(message = "walletId is required")
    private UUID walletId;
    @NotNull(message = "category is required")
    private UUID categoryId;
    @NotNull
    @Positive(message = "Amount must be greater than zero")
    private BigDecimal amount;
    private String note;
    @NotNull(message = "payment method is required")
    private PaymentMethod paymentMethod;
    private UUID userId;
    private LocalDateTime expenseDate;
}

