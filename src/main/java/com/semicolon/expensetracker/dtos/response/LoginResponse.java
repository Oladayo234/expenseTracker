package com.semicolon.expensetracker.dtos.response;

import lombok.Data;

import java.util.UUID;

@Data
public class LoginResponse {
    private String token;
    private String name;
    private String userName;
    private String id;
    private String message;

}
