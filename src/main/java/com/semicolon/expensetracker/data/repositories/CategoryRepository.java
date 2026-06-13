package com.semicolon.expensetracker.data.repositories;

import com.semicolon.expensetracker.data.models.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByUserId(Long userId);
    boolean existsByNameAndUserId(String name, Long userId);
    List<Category> findByUserIdOrDefaultCategoryTrue(Long userId);
    List<Category> findByDefaultCategoryTrue();

    boolean existsByNameAndDefaultCategoryTrue(String name);

    @Query("SELECT c FROM Category c WHERE LOWER(c.name) = LOWER(:name) AND (c.defaultCategory = true OR c.user.id = :userId)")
    Optional<Category> findByNameIgnoreCaseAndUserIdOrDefault(@Param("name") String name, @Param("userId") Long userId);
    Optional<Category> findByNameAndDefaultCategoryTrue(String name);
    Optional<Category> findByPublicId(UUID publicId);
}