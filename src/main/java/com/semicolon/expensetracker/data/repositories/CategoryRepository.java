package com.semicolon.expensetracker.data.repositories;

import com.semicolon.expensetracker.data.models.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {
    List<Category> findByUserId(UUID UserId);
    List<Category> findByIsDefault(boolean isDefault);
}
