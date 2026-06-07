package com.semicolon.expensetracker.data.repositories;

import com.semicolon.expensetracker.data.models.Budget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BudgetRepository extends JpaRepository<Budget, Long> {
    List<Budget> findByUserId(Long userId);
    boolean existsByUserIdAndCategoryIdAndMonthYear(Long userId, Long categoryId, YearMonth monthYear);
    Optional<Budget> findByIdAndUserId(Long id, Long userId);

    Optional<Budget> findByPublicId(UUID publicId);
}
