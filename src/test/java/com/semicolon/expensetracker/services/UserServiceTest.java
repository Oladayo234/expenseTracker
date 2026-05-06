package com.semicolon.expensetracker.services;

import com.semicolon.expensetracker.dtos.request.ChangePasswordRequest;
import com.semicolon.expensetracker.dtos.request.LoginRequest;
import com.semicolon.expensetracker.dtos.request.RegisterUserRequest;
import com.semicolon.expensetracker.dtos.response.LoginResponse;
import com.semicolon.expensetracker.dtos.response.RegisterUserResponse;
import com.semicolon.expensetracker.exceptions.InvalidEntryException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
class UserServiceTest {

    @Autowired UserService userService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private RegisterUserRequest registerRequest(String email, String username) {
        RegisterUserRequest r = new RegisterUserRequest();
        r.setEmail(email);
        r.setUserName(username);
        r.setName("Test User");
        r.setPassword("password123");
        r.setPhoneNumber("08000000000");
        return r;
    }

    private LoginRequest loginRequest(String username, String password) {
        LoginRequest r = new LoginRequest();
        r.setUserName(username);
        r.setPassword(password);
        return r;
    }

    @Test
    void registerUser_duplicateEmail_throwsInvalidEntryException() {
        userService.registerUser(registerRequest("dup@test.com", "dup1"));
        assertThrows(InvalidEntryException.class,
                () -> userService.registerUser(registerRequest("dup@test.com", "dup2")));
    }

    @Test
    void registerUser_success_returnsEmailAndUsername() {
        RegisterUserResponse result = userService.registerUser(registerRequest("new@test.com", "newuser"));
        assertThat(result.getEmail()).isEqualTo("new@test.com");
        assertThat(result.getUserName()).isEqualTo("newuser");
    }

    @Test
    void login_userNotFound_throwsInvalidEntryException() {
        assertThrows(InvalidEntryException.class, () -> userService.login(loginRequest("ghost", "pass")));
    }

    @Test
    void login_wrongPassword_throwsInvalidEntryException() {
        userService.registerUser(registerRequest("login@test.com", "loginuser"));
        assertThrows(InvalidEntryException.class, () -> userService.login(loginRequest("loginuser", "wrongpassword")));
    }

    @Test
    void login_success_returnsTokenAndId() {
        userService.registerUser(registerRequest("tok@test.com", "tokuser"));
        LoginResponse result = userService.login(loginRequest("tokuser", "password123"));
        assertThat(result.getToken()).isNotBlank();
        assertThat(result.getId()).isNotNull();
    }

    @Test
    void changePassword_wrongCurrentPassword_throwsInvalidEntryException() {
        userService.registerUser(registerRequest("cp1@test.com", "cpuser1"));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("cp1@test.com", null));
        assertThrows(InvalidEntryException.class,
                () -> userService.changePassword(new ChangePasswordRequest("wrongPass", "newPass123", "newPass123")));
    }

    @Test
    void changePassword_passwordsMismatch_throwsInvalidEntryException() {
        userService.registerUser(registerRequest("cp2@test.com", "cpuser2"));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("cp2@test.com", null));
        assertThrows(InvalidEntryException.class,
                () -> userService.changePassword(new ChangePasswordRequest("password123", "newPass123", "different123")));
    }

    @Test
    void changePassword_success_allowsLoginWithNewPassword() {
        userService.registerUser(registerRequest("cp3@test.com", "cpuser3"));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("cp3@test.com", null));
        userService.changePassword(new ChangePasswordRequest("password123", "newPass999!", "newPass999!"));
        SecurityContextHolder.clearContext();

        LoginResponse result = userService.login(loginRequest("cpuser3", "newPass999!"));
        assertThat(result.getToken()).isNotBlank();
    }
}