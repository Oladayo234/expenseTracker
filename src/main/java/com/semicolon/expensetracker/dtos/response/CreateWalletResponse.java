package com.semicolon.expensetracker.dtos.response;

import com.semicolon.expensetracker.data.models.enums.Currency;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateWalletResponse {
    private UUID publicId;
    private String name;
    private Currency currency;
    private String message;
}
