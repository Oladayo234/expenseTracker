package com.semicolon.expensetracker.dtos.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateUserRequest {
    private String password;
    private String phoneNumber;
    private String userName;
    @Email
    private String email;
}
