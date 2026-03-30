package com.semicolon.expensetracker.exceptions;

public class WalletDoesNotExistException extends RuntimeException {
    public WalletDoesNotExistException(String message) {
        super(message);
    }
}
