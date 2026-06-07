package com.semicolon.expensetracker.data.repositories;

import com.semicolon.expensetracker.data.models.RecurringExpenses;
import com.semicolon.expensetracker.data.models.Wallet;
import com.semicolon.expensetracker.data.models.enums.Frequency;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RecurringExpensesRepository extends JpaRepository<RecurringExpenses, Long> {
    List<RecurringExpenses> findByWalletIdAndNextDueDateAndFrequency(Long id, LocalDate date, Frequency frequency);
    List<RecurringExpenses> findByUserId(Long userId);

    Optional<RecurringExpenses> findByPublicId(UUID publicId);
}
