package com.semicolon.expensetracker.data.repositories;

import com.semicolon.expensetracker.data.models.Expense;
import com.semicolon.expensetracker.data.models.enums.TransactionType;
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

 @Query("SELECT SUM(e.amount) FROM Expense e WHERE e.wallet.id = :walletId AND e.category.transactionType IN :types")
 Optional<BigDecimal> sumByWalletIdAndTypes(@Param("walletId") UUID walletId, @Param("types") List<TransactionType> types);

 @Query("SELECT SUM(e.amount) FROM Expense e WHERE e.wallet.user.id = :userId AND e.category.id = :categoryId AND e.expenseDate BETWEEN :start AND :end")
 Optional<BigDecimal> sumByCategoryAndUserAndDateRange(@Param("userId") UUID userId, @Param("categoryId") UUID categoryId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

 @Modifying
 @Query("UPDATE Expense e SET e.category = (SELECT c FROM Category c WHERE c.id = :newCategoryId) WHERE e.category.id = :oldCategoryId")
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
