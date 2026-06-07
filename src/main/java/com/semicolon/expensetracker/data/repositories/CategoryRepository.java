package com.semicolon.expensetracker.data.repositories;

import com.semicolon.expensetracker.data.models.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByUserId(Long userId);
    boolean existsByNameAndUserId(String name, Long userId);
    List<Category> findByUserIdOrDefaultCategoryTrue(Long userId);
    List<Category> findByDefaultCategoryTrue();
    Optional<Category> findByNameAndDefaultCategoryTrue(String name);
    Optional<Category> findByPublicId(UUID publicId);
}