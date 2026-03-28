package com.semicolon.expensetracker.dtos.response;

import lombok.Data;

import java.util.UUID;
@Data
public class RegisterUserResponse {
    private String email;
    private String name;
    private String userName;
}
