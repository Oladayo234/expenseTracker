package com.semicolon.expensetracker.dtos.response;

import lombok.Data;

import java.util.UUID;

@Data
public class UpdateUserResponse {
    private UUID id;
    private String username;
    private String token;
}
