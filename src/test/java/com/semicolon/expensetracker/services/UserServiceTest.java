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
import com.semicolon.expensetracker.exceptions.UnauthorizedException;
import com.semicolon.expensetracker.security.JwtService;
import com.semicolon.expensetracker.utils.AuthUtils;
import com.semicolon.expensetracker.utils.mappers.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private BCryptPasswordEncoder bCryptPasswordEncoder;
    @Mock
    private UserRepository userRepository;
    @Mock
    private JwtService jwtService;
    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserService userService;

    // ─── registerUser ────────────────────────────────────────────────────────

    @Test
    void registerUser_newEmailAndUsername_returnsResponse() {
        RegisterUserRequest request = new RegisterUserRequest();
        request.setEmail("test@example.com");
        request.setUserName("testuser");
        request.setPassword("password123");
        request.setName("Test User");
        request.setPhoneNumber("08012345678");
        request.setCurrencyPreference(Currency.NAIRA);

        User mappedUser = new User();
        mappedUser.setPassword("raw");
        User savedUser = new User();
        RegisterUserResponse expected = new RegisterUserResponse();

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());
        when(userMapper.toRegisterUser(request)).thenReturn(mappedUser);
        when(bCryptPasswordEncoder.encode("password123")).thenReturn("encoded");
        when(userRepository.save(mappedUser)).thenReturn(savedUser);
        when(userMapper.toRegisterResponse(savedUser)).thenReturn(expected);

        RegisterUserResponse result = userService.registerUser(request);

        assertThat(result).isEqualTo(expected);
        verify(userRepository).save(mappedUser);
        assertThat(mappedUser.getPassword()).isEqualTo("encoded");
    }

    @Test
    void registerUser_nullCurrencyPreference_defaultsToNaira() {
        RegisterUserRequest request = new RegisterUserRequest();
        request.setEmail("test@example.com");
        request.setUserName("testuser");
        request.setPassword("pass");
        request.setCurrencyPreference(null);

        User mappedUser = new User();
        User savedUser = new User();

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        when(userMapper.toRegisterUser(request)).thenReturn(mappedUser);
        when(bCryptPasswordEncoder.encode(any())).thenReturn("encoded");
        when(userRepository.save(mappedUser)).thenReturn(savedUser);
        when(userMapper.toRegisterResponse(savedUser)).thenReturn(new RegisterUserResponse());

        userService.registerUser(request);

        assertThat(request.getCurrencyPreference()).isEqualTo(Currency.NAIRA);
    }

    @Test
    void registerUser_duplicateEmail_throwsInvalidEntryException() {
        RegisterUserRequest request = new RegisterUserRequest();
        request.setEmail("duplicate@example.com");
        request.setUserName("anyuser");

        when(userRepository.findByEmail("duplicate@example.com")).thenReturn(Optional.of(new User()));

        assertThatThrownBy(() -> userService.registerUser(request))
                .isInstanceOf(InvalidEntryException.class)
                .hasMessage("Email already in use");
    }

    @Test
    void registerUser_duplicateUsername_throwsInvalidEntryException() {
        RegisterUserRequest request = new RegisterUserRequest();
        request.setEmail("unique@example.com");
        request.setUserName("taken");

        when(userRepository.findByEmail("unique@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("taken")).thenReturn(Optional.of(new User()));

        assertThatThrownBy(() -> userService.registerUser(request))
                .isInstanceOf(InvalidEntryException.class)
                .hasMessage("Username already taken");
    }

    // ─── login ───────────────────────────────────────────────────────────────

    @Test
    void login_validCredentials_returnsLoginResponse() {
        LoginRequest request = new LoginRequest();
        request.setUserName("testuser");
        request.setPassword("password123");

        User user = new User();
        user.setPassword("encoded");
        LoginResponse expected = new LoginResponse();

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(bCryptPasswordEncoder.matches("password123", "encoded")).thenReturn(true);
        when(jwtService.generateToken(user)).thenReturn("jwt-token");
        when(userMapper.toLoginResponse(user, "jwt-token")).thenReturn(expected);

        LoginResponse result = userService.login(request);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void login_unknownUsername_throwsUnauthorizedException() {
        LoginRequest request = new LoginRequest();
        request.setUserName("ghost");
        request.setPassword("pass");

        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.login(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Invalid credentials");
    }

    @Test
    void login_wrongPassword_throwsUnauthorizedException() {
        LoginRequest request = new LoginRequest();
        request.setUserName("testuser");
        request.setPassword("wrongpass");

        User user = new User();
        user.setPassword("encoded");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(bCryptPasswordEncoder.matches("wrongpass", "encoded")).thenReturn(false);

        assertThatThrownBy(() -> userService.login(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Invalid credentials");
    }

    // ─── updateUser ──────────────────────────────────────────────────────────

    @Test
    void updateUser_allFieldsProvided_updatesAndReturnsResponse() {
        UpdateUserRequest request = new UpdateUserRequest();
        request.setUserName("newuser");
        request.setPhoneNumber("08099999999");
        request.setEmail("new@example.com");

        User currentUser = new User();
        currentUser.setUsername("old");
        User savedUser = new User();
        UpdateUserResponse expected = new UpdateUserResponse();

        try (MockedStatic<AuthUtils> auth = mockStatic(AuthUtils.class)) {
            auth.when(AuthUtils::getCurrentUser).thenReturn(currentUser);
            when(userRepository.save(currentUser)).thenReturn(savedUser);
            when(jwtService.generateToken(savedUser)).thenReturn("token");
            when(userMapper.toUpdateResponse(savedUser, "token")).thenReturn(expected);

            UpdateUserResponse result = userService.updateUser(request);

            assertThat(result).isEqualTo(expected);
            assertThat(currentUser.getUsername()).isEqualTo("newuser");
            assertThat(currentUser.getPhoneNumber()).isEqualTo("08099999999");
            assertThat(currentUser.getEmail()).isEqualTo("new@example.com");
        }
    }

    @Test
    void updateUser_nullFields_doesNotOverwriteExistingValues() {
        UpdateUserRequest request = new UpdateUserRequest();
        request.setUserName(null);
        request.setPhoneNumber(null);
        request.setEmail(null);

        User currentUser = new User();
        currentUser.setUsername("original");
        currentUser.setPhoneNumber("08000000000");
        currentUser.setEmail("orig@example.com");
        User savedUser = new User();

        try (MockedStatic<AuthUtils> auth = mockStatic(AuthUtils.class)) {
            auth.when(AuthUtils::getCurrentUser).thenReturn(currentUser);
            when(userRepository.save(currentUser)).thenReturn(savedUser);
            when(jwtService.generateToken(savedUser)).thenReturn("token");
            when(userMapper.toUpdateResponse(savedUser, "token")).thenReturn(new UpdateUserResponse());

            userService.updateUser(request);

            assertThat(currentUser.getUsername()).isEqualTo("original");
            assertThat(currentUser.getPhoneNumber()).isEqualTo("08000000000");
            assertThat(currentUser.getEmail()).isEqualTo("orig@example.com");
        }
    }

    // ─── changePassword ──────────────────────────────────────────────────────

    @Test
    void changePassword_correctCurrentPasswordAndMatchingNew_savesNewPassword() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("oldpass");
        request.setNewPassword("newpass123");
        request.setConfirmPassword("newpass123");

        User currentUser = new User();
        currentUser.setPassword("encodedOld");

        try (MockedStatic<AuthUtils> auth = mockStatic(AuthUtils.class)) {
            auth.when(AuthUtils::getCurrentUser).thenReturn(currentUser);
            when(bCryptPasswordEncoder.matches("oldpass", "encodedOld")).thenReturn(true);
            when(bCryptPasswordEncoder.encode("newpass123")).thenReturn("encodedNew");

            userService.changePassword(request);

            verify(userRepository).save(currentUser);
            assertThat(currentUser.getPassword()).isEqualTo("encodedNew");
        }
    }

    @Test
    void changePassword_incorrectCurrentPassword_throwsUnauthorizedException() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("wrong");
        request.setNewPassword("newpass123");
        request.setConfirmPassword("newpass123");

        User currentUser = new User();
        currentUser.setPassword("encodedOld");

        try (MockedStatic<AuthUtils> auth = mockStatic(AuthUtils.class)) {
            auth.when(AuthUtils::getCurrentUser).thenReturn(currentUser);
            when(bCryptPasswordEncoder.matches("wrong", "encodedOld")).thenReturn(false);

            assertThatThrownBy(() -> userService.changePassword(request))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessage("Current password is incorrect");
        }
    }

    @Test
    void changePassword_newPasswordsDoNotMatch_throwsBadRequestException() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("oldpass");
        request.setNewPassword("newpass123");
        request.setConfirmPassword("different");

        User currentUser = new User();
        currentUser.setPassword("encodedOld");

        try (MockedStatic<AuthUtils> auth = mockStatic(AuthUtils.class)) {
            auth.when(AuthUtils::getCurrentUser).thenReturn(currentUser);
            when(bCryptPasswordEncoder.matches("oldpass", "encodedOld")).thenReturn(true);

            assertThatThrownBy(() -> userService.changePassword(request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("New passwords do not match");
        }
    }
}
