package com.semicolon.expensetracker.dtos.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank(message = "password is required")
    private String password;

    @NotBlank(message = "username is required")
    private String userName;
}
