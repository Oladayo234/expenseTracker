package com.semicolon.expensetracker.dtos.response;

import com.semicolon.expensetracker.data.models.enums.Currency;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class WalletBalanceResponse {
    private UUID walletId;
    private String walletName;
    private Currency currency;
    private BigDecimal balance;
}