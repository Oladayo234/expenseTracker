package com.semicolon.expensetracker.dtos.response;

import lombok.Data;

import java.util.UUID;

@Data
public class UpdateUserResponse {
    private UUID publicId;
    private String username;
    private String token;
}
