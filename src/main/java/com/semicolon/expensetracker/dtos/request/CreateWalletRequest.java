package com.semicolon.expensetracker.dtos.request;

import com.semicolon.expensetracker.data.models.User;
import com.semicolon.expensetracker.data.models.enums.Currency;
import jakarta.persistence.Entity;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.UUID;


@Data
public class CreateWalletRequest {
    private UUID userId;
    @NotBlank(message = "wallet name is required")
    private String name;
    private Currency currency;


}
