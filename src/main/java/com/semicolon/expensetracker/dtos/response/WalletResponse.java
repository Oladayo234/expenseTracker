package com.semicolon.expensetracker.dtos.response;

import com.semicolon.expensetracker.data.models.enums.Currency;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class WalletResponse {
    private UUID id;
    private String name;
    private Currency currency;
}