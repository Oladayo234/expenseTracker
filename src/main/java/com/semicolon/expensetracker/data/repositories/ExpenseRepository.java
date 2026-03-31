package com.semicolon.expensetracker.data.repositories;

import com.semicolon.expensetracker.data.models.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExpenseRepository extends JpaRepository<Expense, UUID> {
 List<Expense> findByWalletId(UUID WalletId);
 List<Expense> findByCategoryId(UUID CategoryId);
 @Query("SELECT SUM(e.amount) FROM Expense e WHERE e.wallet.id = :walletId")
 Optional<BigDecimal> sumAmountByWalletId(@Param("walletId") UUID walletId);
 @Modifying
 @Query("UPDATE Expense e SET e.category.id = :newCategoryId WHERE e.category.id = :oldCategoryId")
 int reassignExpensesToCategory(@Param("oldCategoryId") UUID oldCategoryId, @Param("newCategoryId") UUID newCategoryId);
 List<Expense> findByWalletIdOrderByExpenseDateDesc(UUID walletId);
 List<Expense> findByCategoryIdOrderByExpenseDateDesc(UUID categoryId);
 List<Expense> findByWalletIdAndExpenseDateBetweenOrderByExpenseDateDesc(UUID walletId, LocalDateTime start, LocalDateTime end);
 List<Expense> findByWalletUserIdAndExpenseDateBetween(UUID userId, LocalDateTime start, LocalDateTime end);
 @Query("SELECT e.category.id, e.category.name, e.category.transactionType, SUM(e.amount) FROM Expense e WHERE e.wallet.user.id = :userId AND e.expenseDate BETWEEN :start AND :end GROUP BY e.category.id, e.category.name, e.category.transactionType")
 List<Object[]> findCategoryBreakdownByUserIdAndDateRange(@Param("userId") UUID userId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
 List<Expense> findByExpenseDateBetween(LocalDateTime start, LocalDateTime end);
 List<Expense> findByWalletIdAndExpenseDateBetween(UUID walletId, LocalDateTime start, LocalDateTime end);
}
