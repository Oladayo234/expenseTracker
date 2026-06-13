package com.semicolon.expensetracker.config;

import com.semicolon.expensetracker.data.models.Expense;
import com.semicolon.expensetracker.data.models.RecurringExpenses;
import com.semicolon.expensetracker.data.models.enums.PaymentMethod;
import com.semicolon.expensetracker.data.repositories.ExpenseRepository;
import com.semicolon.expensetracker.data.repositories.RecurringExpensesRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RecurringExpenseScheduler {

    private final RecurringExpensesRepository recurringExpensesRepository;
    private final ExpenseRepository expenseRepository;

    // Runs every day at 8:00 AM
    @Scheduled(cron = "0 0 8 * * *")
    @Transactional
    public void processRecurringExpenses() {
        LocalDate today = LocalDate.now();
        List<RecurringExpenses> due = recurringExpensesRepository
                .findByNextDueDateLessThanEqual(today);

        if (due.isEmpty()) {
            log.info("RecurringExpenseScheduler: no expenses due today");
            return;
        }

        int processed = 0;
        for (RecurringExpenses recurring : due) {
            try {
                // Auto-log the expense
                Expense expense = new Expense();
                expense.setWallet(recurring.getWallet());
                expense.setCategory(recurring.getCategory());
                expense.setAmount(recurring.getAmount());
                expense.setNote("Auto-logged: " + recurring.getCategory().getName());
                expense.setPaymentMethod(PaymentMethod.TRANSFER);
                expense.setExpenseDate(LocalDateTime.now());
                expenseRepository.save(expense);

                // Advance nextDueDate based on frequency
                LocalDate nextDate = switch (recurring.getFrequency()) {
                    case DAILY     -> today.plusDays(1);
                    case WEEKLY    -> today.plusWeeks(1);
                    case MONTHLY   -> today.plusMonths(1);
                    case QUARTERLY -> today.plusMonths(3);
                    case YEARLY    -> today.plusYears(1);
                };

                recurring.setNextDueDate(nextDate);
                recurringExpensesRepository.save(recurring);
                processed++;

            } catch (Exception e) {
                log.error("RecurringExpenseScheduler: failed to process recurring " +
                        "expense id={}, error={}", recurring.getId(), e.getMessage());
            }
        }

        log.info("RecurringExpenseScheduler: processed {} recurring expenses", processed);
    }
}