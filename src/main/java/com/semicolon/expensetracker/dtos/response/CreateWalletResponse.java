package com.semicolon.expensetracker.dtos.response;

import com.semicolon.expensetracker.data.models.enums.Currency;
import lombok.Data;

@Data
public class CreateWalletResponse {
    private String name;
    private Currency currency;
}
