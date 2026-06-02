package com.semicolon.expensetracker.controllers;

import com.semicolon.expensetracker.dtos.request.CreateCategoryRequest;
import com.semicolon.expensetracker.dtos.request.DeleteCategoryRequest;
import com.semicolon.expensetracker.services.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping
    public ResponseEntity<?> createCategory(@Valid @RequestBody CreateCategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(categoryService.createCategory(request));
    }

    @GetMapping
    public ResponseEntity<?> getCategories() {
        return ResponseEntity.ok(categoryService.getCategories());
    }

    @DeleteMapping
    public ResponseEntity<?> deleteCategory(@Valid @RequestBody DeleteCategoryRequest request) {
        categoryService.deleteCategory(request);
        return ResponseEntity.ok("Category deleted successfully");
    }
}