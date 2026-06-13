package com.semicolon.expensetracker.whatsapp;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class MessageParser {

    // Matches: add 5000 food gtbank
    //          add 5,000 transport opay note goes here
    private static final Pattern ADD_PATTERN = Pattern.compile(
            "^add\\s+(\\d+(?:[.,]\\d+)?)\\s+(\\w+)\\s+(\\w+)(?:\\s+(.+))?$",
            Pattern.CASE_INSENSITIVE
    );

    public ParsedIntent parse(String message) {
        if (message == null || message.isBlank()) {
            return ParsedIntent.unknown();
        }

        String trimmed = message.trim().toLowerCase();

        // --- View commands ---
        if (trimmed.equals("summary"))  return ParsedIntent.of(Intent.GET_SUMMARY);
        if (trimmed.equals("budgets"))  return ParsedIntent.of(Intent.GET_BUDGETS);
        if (trimmed.equals("wallets"))  return ParsedIntent.of(Intent.GET_WALLETS);
        if (trimmed.equals("expenses")) return ParsedIntent.of(Intent.GET_EXPENSES);
        if (trimmed.equals("help"))     return ParsedIntent.of(Intent.HELP);

        // --- Add expense ---
        Matcher matcher = ADD_PATTERN.matcher(trimmed);
        if (matcher.matches()) {
            String rawAmount = matcher.group(1).replace(",", "");
            String category  = matcher.group(2);
            String wallet    = matcher.group(3);
            String note      = matcher.group(4); // nullable

            try {
                BigDecimal amount = new BigDecimal(rawAmount);
                return ParsedIntent.addExpense(amount, category, wallet, note);
            } catch (NumberFormatException e) {
                return ParsedIntent.unknown();
            }
        }

        return ParsedIntent.unknown();
    }
}