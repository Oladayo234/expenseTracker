package com.semicolon.expensetracker.data.repositories;

import com.semicolon.expensetracker.data.models.Expense;
import com.semicolon.expensetracker.data.models.enums.TransactionType;
import com.semicolon.expensetracker.dtos.response.CategoryBreakdownResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

 Optional<Expense> findByPublicId(UUID publicId);

 @Query("SELECT SUM(e.amount) FROM Expense e WHERE e.wallet.id = :walletId AND e.category.transactionType IN :types")
 Optional<BigDecimal> sumByWalletIdAndTypes(@Param("walletId") Long walletId, @Param("types") List<TransactionType> types);

 @Query("SELECT SUM(e.amount) FROM Expense e WHERE e.wallet.user.id = :userId AND e.category.id = :categoryId AND e.expenseDate BETWEEN :start AND :end")
 Optional<BigDecimal> sumByCategoryAndUserAndDateRange(@Param("userId") Long userId, @Param("categoryId") Long categoryId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

 @Modifying
 @Query("UPDATE Expense e SET e.category = (SELECT c FROM Category c WHERE c.id = :newCategoryId) WHERE e.category.id = :oldCategoryId")
 int reassignExpensesToCategory(@Param("oldCategoryId") Long oldCategoryId, @Param("newCategoryId") Long newCategoryId);

 List<Expense> findByWalletIdOrderByExpenseDateDesc(Long walletId);

 List<Expense> findByCategoryIdOrderByExpenseDateDesc(Long categoryId);

 List<Expense> findByWalletIdAndExpenseDateBetweenOrderByExpenseDateDesc(Long walletId, LocalDateTime start, LocalDateTime end);

 List<Expense> findByWalletUserIdAndExpenseDateBetween(Long userId, LocalDateTime start, LocalDateTime end);

 @Query("SELECT new com.semicolon.expensetracker.dtos.response.CategoryBreakdownResponse(" +
         "e.category.publicId, e.category.name, SUM(e.amount)) " +
         "FROM Expense e " +
         "WHERE e.wallet.user.id = :userId " +
         "AND e.expenseDate BETWEEN :start AND :end " +
         "GROUP BY e.category.publicId, e.category.name")
 List<CategoryBreakdownResponse> findCategoryBreakdownByUserIdAndDateRange(
         @Param("userId") Long userId,
         @Param("start") LocalDateTime start,
         @Param("end") LocalDateTime end);
}