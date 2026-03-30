package com.semicolon.expensetracker.utils.mappers;

import com.semicolon.expensetracker.data.models.User;
import com.semicolon.expensetracker.dtos.request.RegisterUserRequest;
import com.semicolon.expensetracker.dtos.response.LoginResponse;
import com.semicolon.expensetracker.dtos.response.RegisterUserResponse;
import com.semicolon.expensetracker.dtos.response.UpdateUserResponse;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public User toEntity(RegisterUserRequest request) {
        User user = new User();
        user.setName(request.getName());
        user.setUsername(request.getUserName());
        user.setEmail(request.getEmail());
        user.setPassword(request.getPassword());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setCurrencyPreference(request.getCurrencyPreference());
        return user;
    }

    public RegisterUserResponse toRegisterResponse(User user) {
        RegisterUserResponse response = new RegisterUserResponse();
        response.setEmail(user.getEmail());
        response.setName(user.getName());
        response.setUserName(user.getUsername());
        return response;
    }

    public LoginResponse toLoginResponse(User user, String token) {
        LoginResponse response = new LoginResponse();
        response.setId(user.getId().toString());
        response.setName(user.getName());
        response.setUserName(user.getUsername());
        response.setToken(token);
        response.setMessage("Login successful");
        return response;
    }

    public UpdateUserResponse toUpdateResponse(User user, String token) {
        UpdateUserResponse response = new UpdateUserResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setToken(token);
        return response;
    }
}