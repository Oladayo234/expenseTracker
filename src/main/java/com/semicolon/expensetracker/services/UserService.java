package com.semicolon.expensetracker.services;

import com.semicolon.expensetracker.data.models.User;
import com.semicolon.expensetracker.data.models.enums.Currency;
import com.semicolon.expensetracker.data.repositories.UserRepository;
import com.semicolon.expensetracker.dtos.request.ChangePasswordRequest;
import com.semicolon.expensetracker.dtos.request.LoginRequest;
import com.semicolon.expensetracker.dtos.request.RegisterUserRequest;
import com.semicolon.expensetracker.dtos.request.UpdateUserRequest;
import com.semicolon.expensetracker.dtos.response.LoginResponse;
import com.semicolon.expensetracker.dtos.response.RegisterUserResponse;
import com.semicolon.expensetracker.dtos.response.UpdateUserResponse;
import com.semicolon.expensetracker.exceptions.InvalidEntryException;
import com.semicolon.expensetracker.security.JwtService;
import com.semicolon.expensetracker.utils.AuthUtils;
import com.semicolon.expensetracker.utils.mappers.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final UserMapper userMapper;

    public RegisterUserResponse registerUser(RegisterUserRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new InvalidEntryException("Email already in use");
        }
        if (userRepository.findByUsername(request.getUserName()).isPresent()) {
            throw new InvalidEntryException("Username already taken");
        }
        if (request.getCurrencyPreference() == null) {
            request.setCurrencyPreference(Currency.NAIRA);
        }
        User user = userMapper.toRegisterUser(request);
        user.setPassword(bCryptPasswordEncoder.encode(request.getPassword()));
        return userMapper.toRegisterResponse(userRepository.save(user));
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUserName())
                .orElseThrow(() -> new InvalidEntryException("Invalid credentials"));
        if (!bCryptPasswordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidEntryException("Invalid credentials");
        }
        return userMapper.toLoginResponse(user, jwtService.generateToken(user));
    }

    public UpdateUserResponse updateUser(UpdateUserRequest request) {
        User user = AuthUtils.getCurrentUser();
        if (request.getUserName() != null) user.setUsername(request.getUserName());
        if (request.getPhoneNumber() != null) user.setPhoneNumber(request.getPhoneNumber());
        if (request.getEmail() != null) user.setEmail(request.getEmail());
        User saved = userRepository.save(user);
        return userMapper.toUpdateResponse(saved, jwtService.generateToken(saved));
    }

    public void changePassword(ChangePasswordRequest request) {
        User user = AuthUtils.getCurrentUser();
        if (!bCryptPasswordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new InvalidEntryException("Current password is incorrect");
        }
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new InvalidEntryException("New passwords do not match");
        }
        user.setPassword(bCryptPasswordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }
}