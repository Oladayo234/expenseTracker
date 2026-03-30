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


@Service
@RequiredArgsConstructor
public class UserService {
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final UserMapper userMapper;

    public RegisterUserResponse registerUser(RegisterUserRequest request){
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new InvalidEntryException("User or Email already exists");
        }
        if (request.getCurrencyPreference() == null) {
            request.setCurrencyPreference(Currency.NAIRA);
        }
        User user = userMapper.toEntity(request);
        user.setPassword(bCryptPasswordEncoder.encode(request.getPassword()));
        User savedUser = userRepository.save(user);
        return userMapper.toRegisterResponse(savedUser);
    }

    public LoginResponse login(LoginRequest request){
        User user = userRepository.findByUsername(request.getUserName())
                .orElseThrow(() -> new InvalidEntryException("Email or username incorrect"));

        if (!bCryptPasswordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidEntryException("Wrong password");
        }
        return userMapper.toLoginResponse(user, jwtService.generateToken(user));
    }

    public UpdateUserResponse updateUser(UpdateUserRequest request){
        User user = userRepository.findById(request.getId())
                .orElseThrow(() -> new InvalidEntryException("User not found"));

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
