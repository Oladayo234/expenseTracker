package com.semicolon.expensetracker.utils;

import com.semicolon.expensetracker.data.models.User;
import com.semicolon.expensetracker.exceptions.BadRequestException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class AuthUtils {

    private AuthUtils() {}

    public static User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof User)) {
            throw new BadRequestException("User is not authenticated");
        }
        return (User) authentication.getPrincipal();
    }

    public static Long getCurrentUserId() {
        return getCurrentUser().getId();
    }
}