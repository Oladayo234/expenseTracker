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
import com.semicolon.expensetracker.utils.mappers.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final UserMapper userMapper;

    public RegisterUserResponse registerUser(RegisterUserRequest request) {
        validateEmailNotExists(request.getEmail());
        setDefaultCurrencyIfNull(request);
        User user = createUserEntity(request);
        User savedUser = saveUser(user);
        return userMapper.toRegisterResponse(savedUser);
    }

    public LoginResponse login(LoginRequest request) {
        User user = findUserByUsername(request.getUserName());
        validatePassword(request.getPassword(), user.getPassword());
        return userMapper.toLoginResponse(user, jwtService.generateToken(user));
    }

    public UpdateUserResponse updateUser(UpdateUserRequest request) {
        User user = findUserById(request.getId());
        updateUserFields(request, user);
        User savedUser = saveUser(user);
        return userMapper.toUpdateResponse(savedUser, jwtService.generateToken(savedUser));
    }

    private void validateEmailNotExists(String email) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new InvalidEntryException("User or Email already exists");
        }
    }

    private void setDefaultCurrencyIfNull(RegisterUserRequest request) {
        if (request.getCurrencyPreference() == null) {
            request.setCurrencyPreference(Currency.NAIRA);
        }
    }

    private void validatePassword(String rawPassword, String encodedPassword) {
        if (!bCryptPasswordEncoder.matches(rawPassword, encodedPassword)) {
            throw new InvalidEntryException("Wrong password");
        }
    }

    private User findUserById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new InvalidEntryException("User not found"));
    }

    private User findUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new InvalidEntryException("Email or username incorrect"));
    }

    private User createUserEntity(RegisterUserRequest request) {
        User user = userMapper.toEntity(request);
        user.setPassword(encodePassword(request.getPassword()));
        return user;
    }

    private void updateUserFields(UpdateUserRequest request, User user) {
        if (request.getPassword() != null) {
            user.setPassword(encodePassword(request.getPassword()));
        }
        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
    }

    private String encodePassword(String rawPassword) {
        return bCryptPasswordEncoder.encode(rawPassword);
    }

    private User saveUser(User user) {
        return userRepository.save(user);
    }
}