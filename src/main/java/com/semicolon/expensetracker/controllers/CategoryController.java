package com.semicolon.expensetracker.controllers;

import com.semicolon.expensetracker.dtos.request.CreateCategoryRequest;
import com.semicolon.expensetracker.dtos.request.DeleteCategoryRequest;
import com.semicolon.expensetracker.services.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {
    private final CategoryService categoryService;

    @PostMapping
    public ResponseEntity<?> createCategory(@Valid @RequestBody CreateCategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryService.createCategory(request));
    }

    @GetMapping
    public ResponseEntity<?> getCategories(@RequestParam(required = false) UUID userId) {
        return ResponseEntity.ok(categoryService.getCategories(userId));
    }

    @DeleteMapping
    public ResponseEntity<?> deleteCategory(@RequestBody DeleteCategoryRequest request) {
        categoryService.deleteCategory(request);
        return ResponseEntity.ok("Category deleted successfully");
    }
}
