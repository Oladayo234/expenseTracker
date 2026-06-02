package com.semicolon.expensetracker.dtos.request;

import com.semicolon.expensetracker.data.models.enums.Currency;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateWalletRequest {
    @NotBlank(message = "wallet name is required")
    private String name;
    private Currency currency;
}