package com.semicolon.expensetracker.whatsapp;

import lombok.Getter;
import java.math.BigDecimal;

@Getter
public class ParsedIntent {
    private final Intent intent;
    private final BigDecimal amount;
    private final String category;
    private final String wallet;
    private final String note;

    private ParsedIntent(Intent intent, BigDecimal amount,
                         String category, String wallet, String note) {
        this.intent   = intent;
        this.amount   = amount;
        this.category = category;
        this.wallet   = wallet;
        this.note     = note;
    }

    public static ParsedIntent of(Intent intent) {
        return new ParsedIntent(intent, null, null, null, null);
    }

    public static ParsedIntent addExpense(BigDecimal amount,
                                          String category,
                                          String wallet,
                                          String note) {
        return new ParsedIntent(Intent.ADD_EXPENSE, amount, category, wallet, note);
    }

    public static ParsedIntent unknown() {
        return new ParsedIntent(Intent.UNKNOWN, null, null, null, null);
    }
}