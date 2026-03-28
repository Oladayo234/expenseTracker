package com.semicolon.expensetracker.services;

import com.semicolon.expensetracker.data.models.User;
import com.semicolon.expensetracker.data.models.enums.Currency;
import com.semicolon.expensetracker.data.repositories.UserRepository;
import com.semicolon.expensetracker.dtos.request.LoginRequest;
import com.semicolon.expensetracker.dtos.request.RegisterUserRequest;
import com.semicolon.expensetracker.dtos.request.UpdateUserRequest;
import com.semicolon.expensetracker.dtos.response.LoginResponse;
import com.semicolon.expensetracker.dtos.response.RegisterUserResponse;
import com.semicolon.expensetracker.dtos.response.UpdateUserResponse;
import com.semicolon.expensetracker.exceptions.InvalidEntryException;
import com.semicolon.expensetracker.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    public RegisterUserResponse registerUser(RegisterUserRequest request){
        User user = new User();
        if(userRepository.findByEmail(request.getEmail()).isPresent()){
            throw new InvalidEntryException("User or Email already exists");
        }
        else {
            user.setEmail(request.getEmail());
        }
        String encodedPassword = bCryptPasswordEncoder.encode(request.getPassword());
        user.setPassword(encodedPassword);
        user.setName(request.getName());
        user.setUsername(request.getUserName());
        user.setCurrencyPreference(
                request.getCurrencyPreference() != null ? request.getCurrencyPreference() : Currency.NAIRA
        );
        user.setPhoneNumber(request.getPhoneNumber());
        User savedUser = userRepository.save(user);

        RegisterUserResponse response = new RegisterUserResponse();
        response.setEmail(savedUser.getEmail());
        response.setUserName(savedUser.getUsername());
        response.setName(savedUser.getName());

        return response;
    }

    public LoginResponse login(LoginRequest request){
        User user = userRepository.findByUsername(request.getUserName())
                .orElseThrow(() -> new InvalidEntryException("Email or username incorrect"));

        boolean isPasswordMatch = bCryptPasswordEncoder.matches(request.getPassword(), user.getPassword());
        if (!isPasswordMatch){
            throw new InvalidEntryException("Wrong password");
        }
        LoginResponse response = new LoginResponse();
        response.setToken(jwtService.generateToken(user));
        response.setUserName(user.getUsername());
        response.setName(user.getName());
        response.setId(user.getId().toString());
        response.setMessage("Login successful");
        return response;
    }

    public UpdateUserResponse updateUser(UpdateUserRequest request){
        User user = userRepository.findByUsername(request.getUserName())
                .orElseThrow(() -> new InvalidEntryException("Email or username incorrect"));

        if (request.getPassword() != null) user.setPassword(bCryptPasswordEncoder.encode(request.getPassword()));
        if (request.getPhoneNumber() != null) user.setPhoneNumber(request.getPhoneNumber());
        if (request.getEmail() != null) user.setEmail(request.getEmail());

        User savedUser = userRepository.save(user);

        UpdateUserResponse response = new UpdateUserResponse();
        response.setId(savedUser.getId());
        response.setUsername(savedUser.getUsername());
        response.setToken(jwtService.generateToken(savedUser));
        return response;
    }
}
