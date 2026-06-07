package com.semicolon.expensetracker.dtos.response;

import com.semicolon.expensetracker.data.models.enums.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddExpenseResponse {
    private UUID publicId;
    private UUID walletId;
    private UUID categoryId;
    private String categoryName;
    private BigDecimal amount;
    private String note;
    private PaymentMethod paymentMethod;
    private LocalDateTime expenseDate;
    private String message;

}
