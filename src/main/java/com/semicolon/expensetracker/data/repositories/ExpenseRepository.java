package com.semicolon.expensetracker.data.repositories;

import com.semicolon.expensetracker.data.models.Expense;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface ExpenseRepository extends JpaRepository<Expense, UUID> {
 List<Expense> findByWalletId(UUID WalletId);
 List<Expense> findByCategoryId(UUID CategoryId);
 List<Expense> findByEntryDateBetween(LocalDateTime start, LocalDateTime end);
 List<Expense> findByWalletIdAndEntryDateBetween(UUID WalletId, LocalDateTime start, LocalDateTime end);
}
