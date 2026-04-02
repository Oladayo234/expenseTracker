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
import com.semicolon.expensetracker.exceptions.BadRequestException;
import com.semicolon.expensetracker.exceptions.InvalidEntryException;
import com.semicolon.expensetracker.exceptions.ResourceNotFoundException;
import com.semicolon.expensetracker.security.JwtService;
import com.semicolon.expensetracker.utils.mappers.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
            throw new InvalidEntryException("User or Email already exists");
        }
        if (request.getCurrencyPreference() == null) {
            request.setCurrencyPreference(Currency.NAIRA);
        }
        User user = userMapper.toEntity(request);
        user.setPassword(encodePassword(request.getPassword()));
        return userMapper.toRegisterResponse(userRepository.save(user));
    }

    public LoginResponse login(LoginRequest request) {
        User user = findUserByUsername(request.getUserName());
        if (!bCryptPasswordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidEntryException("Wrong password");
        }
        return userMapper.toLoginResponse(user, jwtService.generateToken(user));
    }

    public UpdateUserResponse updateUser(UpdateUserRequest request) {
        User user = getCurrentUser();
        updateUserFields(request, user);
        User savedUser = userRepository.save(user);
        return userMapper.toUpdateResponse(savedUser, jwtService.generateToken(savedUser));
    }

    public void changePassword(ChangePasswordRequest request) {
        User user = getCurrentUser();
        if (!bCryptPasswordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new InvalidEntryException("Current password is incorrect");
        }
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new InvalidEntryException("New passwords do not match");
        }
        user.setPassword(bCryptPasswordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    private User findUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new InvalidEntryException("Email or username incorrect"));
    }

    private void updateUserFields(UpdateUserRequest request, User user) {
        if (request.getUserName() != null) {
            user.setUsername(request.getUserName());
        }
        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BadRequestException("User is not authenticated");
        }
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private String encodePassword(String rawPassword) {
        return bCryptPasswordEncoder.encode(rawPassword);
    }
}