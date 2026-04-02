package com.semicolon.expensetracker.data.repositories;

import com.semicolon.expensetracker.data.models.Budget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BudgetRepository extends JpaRepository<Budget, UUID> {
    List<Budget> findByUserId(UUID userId);
}
