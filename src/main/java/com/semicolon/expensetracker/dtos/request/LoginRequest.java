package com.semicolon.expensetracker.dtos.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank
    private String password;

    @NotBlank
    private String userName;
}
