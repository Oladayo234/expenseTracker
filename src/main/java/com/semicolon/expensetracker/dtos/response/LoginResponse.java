package com.semicolon.expensetracker.dtos.response;

import lombok.Data;

@Data
public class LoginResponse {
    private String token;
    private String name;
    private String userName;
    private String id;
    private String message;

}
