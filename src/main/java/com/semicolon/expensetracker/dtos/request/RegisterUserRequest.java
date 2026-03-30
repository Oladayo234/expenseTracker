package com.semicolon.expensetracker.dtos.request;

import com.semicolon.expensetracker.data.models.enums.Currency;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RegisterUserRequest {
    @NotBlank(message = "password is required")
    private String password;

    @NotBlank(message = "name is required")
    private String name;

    @NotBlank(message = "email is required")
    @Email
    private String email;

    @NotBlank(message = "username is required")
    private String userName;

    @NotBlank(message = "phone number is required")
    private String phoneNumber;
    private Currency currencyPreference;
}
