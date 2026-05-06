package com.semicolon.expensetracker.data.repositories;

import com.semicolon.expensetracker.data.models.Budget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BudgetRepository extends JpaRepository<Budget, UUID> {
    List<Budget> findByUserId(UUID userId);
    boolean existsByUserIdAndCategoryIdAndMonthYear(UUID userId, UUID categoryId, YearMonth monthYear);
    Optional<Budget> findByIdAndUserId(UUID id, UUID userId);
}
