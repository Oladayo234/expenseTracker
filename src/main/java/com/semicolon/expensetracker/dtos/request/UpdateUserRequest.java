package com.semicolon.expensetracker.dtos.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

@Data
public class UpdateUserRequest {
    private UUID userId;

    @Size(min = 3, max = 20, message = "Username must be between 3 and 20 characters")
    private String userName;

    @Pattern(regexp = "^\\+?[0-9]{10,14}$", message = "Invalid phone number")
    private String phoneNumber;

    @Email(message = "Invalid email address")
    private String email;
}
