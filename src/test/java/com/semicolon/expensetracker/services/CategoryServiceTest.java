package com.semicolon.expensetracker.services;

import com.semicolon.expensetracker.data.models.Category;
import com.semicolon.expensetracker.data.models.User;
import com.semicolon.expensetracker.data.models.enums.TransactionType;
import com.semicolon.expensetracker.data.repositories.CategoryRepository;
import com.semicolon.expensetracker.data.repositories.ExpenseRepository;
import com.semicolon.expensetracker.dtos.request.CreateCategoryRequest;
import com.semicolon.expensetracker.dtos.request.DeleteCategoryRequest;
import com.semicolon.expensetracker.dtos.response.CreateCategoryResponse;
import com.semicolon.expensetracker.exceptions.BadRequestException;
import com.semicolon.expensetracker.exceptions.ForbiddenException;
import com.semicolon.expensetracker.exceptions.InvalidEntryException;
import com.semicolon.expensetracker.exceptions.ResourceNotFoundException;
import com.semicolon.expensetracker.utils.AuthUtils;
import com.semicolon.expensetracker.utils.mappers.CategoryMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private ExpenseRepository expenseRepository;
    @Mock
    private CategoryMapper categoryMapper;

    @InjectMocks
    private CategoryService categoryService;

    // ─── createCategory ───────────────────────────────────────────────────────

    @Test
    void createCategory_newCategoryName_savesAndReturnsResponse() {
        User currentUser = userWithId(1L);
        CreateCategoryRequest request = new CreateCategoryRequest();
        request.setName("Travel");
        request.setTransactionType(TransactionType.OUTFLOW);

        Category savedCategory = new Category();
        CreateCategoryResponse expected = CreateCategoryResponse.builder()
                .name("Travel").build();

        try (MockedStatic<AuthUtils> auth = mockStatic(AuthUtils.class)) {
            auth.when(AuthUtils::getCurrentUser).thenReturn(currentUser);
            when(categoryRepository.existsByNameAndUserId("Travel", 1L)).thenReturn(false);

            ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
            when(categoryRepository.save(captor.capture())).thenReturn(savedCategory);
            when(categoryMapper.toResponse(savedCategory)).thenReturn(expected);

            CreateCategoryResponse result = categoryService.createCategory(request);

            assertThat(result).isEqualTo(expected);
            Category captured = captor.getValue();
            assertThat(captured.getName()).isEqualTo("Travel");
            assertThat(captured.getTransactionType()).isEqualTo(TransactionType.OUTFLOW);
            assertThat(captured.isDefaultCategory()).isFalse();
            assertThat(captured.getUser()).isEqualTo(currentUser);
        }
    }

    @Test
    void createCategory_duplicateName_throwsInvalidEntryException() {
        User currentUser = userWithId(1L);
        CreateCategoryRequest request = new CreateCategoryRequest();
        request.setName("Food");
        request.setTransactionType(TransactionType.OUTFLOW);

        try (MockedStatic<AuthUtils> auth = mockStatic(AuthUtils.class)) {
            auth.when(AuthUtils::getCurrentUser).thenReturn(currentUser);
            when(categoryRepository.existsByNameAndUserId("Food", 1L)).thenReturn(true);

            assertThatThrownBy(() -> categoryService.createCategory(request))
                    .isInstanceOf(InvalidEntryException.class)
                    .hasMessageContaining("Food");
        }
    }

    // ─── getCategories ────────────────────────────────────────────────────────

    @Test
    void getCategories_returnsUserAndDefaultCategories() {
        Long userId = 1L;
        Category c1 = customCategoryOwnedBy(1L, userWithId(userId));
        Category c2 = customCategoryOwnedBy(2L, userWithId(userId));
        CreateCategoryResponse r1 = CreateCategoryResponse.builder().name("Custom").build();
        CreateCategoryResponse r2 = CreateCategoryResponse.builder().name("Default").build();

        try (MockedStatic<AuthUtils> auth = mockStatic(AuthUtils.class)) {
            auth.when(AuthUtils::getCurrentUserId).thenReturn(userId);
            when(categoryRepository.findByUserIdOrDefaultCategoryTrue(userId))
                    .thenReturn(List.of(c1, c2));
            when(categoryMapper.toResponse(c1)).thenReturn(r1);
            when(categoryMapper.toResponse(c2)).thenReturn(r2);

            List<CreateCategoryResponse> result = categoryService.getCategories();

            assertThat(result).containsExactly(r1, r2);
        }
    }

    @Test
    void getCategories_noCategories_returnsEmptyList() {
        try (MockedStatic<AuthUtils> auth = mockStatic(AuthUtils.class)) {
            auth.when(AuthUtils::getCurrentUserId).thenReturn(1L);
            when(categoryRepository.findByUserIdOrDefaultCategoryTrue(1L)).thenReturn(List.of());

            List<CreateCategoryResponse> result = categoryService.getCategories();

            assertThat(result).isEmpty();
        }
    }

    // ─── deleteCategory ───────────────────────────────────────────────────────

    @Test
    void deleteCategory_ownedNonDefaultCategory_reassignsExpensesAndDeletes() {
        Long userId = 1L;
        UUID categoryPublicId = UUID.randomUUID();

        User owner = userWithId(userId);
        Category category = customCategoryOwnedBy(10L, owner);

        Category uncategorized = new Category();
        uncategorized.setId(99L);

        DeleteCategoryRequest request = new DeleteCategoryRequest();
        request.setCategoryId(categoryPublicId);

        try (MockedStatic<AuthUtils> auth = mockStatic(AuthUtils.class)) {
            auth.when(AuthUtils::getCurrentUserId).thenReturn(userId);
            when(categoryRepository.findByPublicId(categoryPublicId))
                    .thenReturn(Optional.of(category));
            when(categoryRepository.findByNameAndDefaultCategoryTrue("Uncategorized"))
                    .thenReturn(Optional.of(uncategorized));

            categoryService.deleteCategory(request);

            verify(expenseRepository).reassignExpensesToCategory(10L, 99L);
            verify(categoryRepository).delete(category);
        }
    }

    @Test
    void deleteCategory_uncategorizedDoesNotExist_createsItThenReassigns() {
        Long userId = 1L;
        UUID categoryPublicId = UUID.randomUUID();

        User owner = userWithId(userId);
        Category category = customCategoryOwnedBy(10L, owner);

        Category newUncategorized = new Category();
        newUncategorized.setId(99L);

        DeleteCategoryRequest request = new DeleteCategoryRequest();
        request.setCategoryId(categoryPublicId);

        try (MockedStatic<AuthUtils> auth = mockStatic(AuthUtils.class)) {
            auth.when(AuthUtils::getCurrentUserId).thenReturn(userId);
            when(categoryRepository.findByPublicId(categoryPublicId))
                    .thenReturn(Optional.of(category));
            when(categoryRepository.findByNameAndDefaultCategoryTrue("Uncategorized"))
                    .thenReturn(Optional.empty());

            ArgumentCaptor<Category> saveCaptor = ArgumentCaptor.forClass(Category.class);
            when(categoryRepository.save(saveCaptor.capture())).thenReturn(newUncategorized);

            categoryService.deleteCategory(request);

            Category savedUncategorized = saveCaptor.getValue();
            assertThat(savedUncategorized.getName()).isEqualTo("Uncategorized");
            assertThat(savedUncategorized.isDefaultCategory()).isTrue();
            assertThat(savedUncategorized.getUser()).isNull();

            verify(expenseRepository).reassignExpensesToCategory(10L, 99L);
            verify(categoryRepository).delete(category);
        }
    }

    @Test
    void deleteCategory_categoryNotFound_throwsResourceNotFoundException() {
        UUID categoryPublicId = UUID.randomUUID();
        DeleteCategoryRequest request = new DeleteCategoryRequest();
        request.setCategoryId(categoryPublicId);

        try (MockedStatic<AuthUtils> auth = mockStatic(AuthUtils.class)) {
            auth.when(AuthUtils::getCurrentUserId).thenReturn(1L);
            when(categoryRepository.findByPublicId(categoryPublicId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> categoryService.deleteCategory(request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Category does not exist");
        }
    }

    @Test
    void deleteCategory_defaultCategory_throwsBadRequestException() {
        UUID categoryPublicId = UUID.randomUUID();
        DeleteCategoryRequest request = new DeleteCategoryRequest();
        request.setCategoryId(categoryPublicId);

        Category defaultCat = new Category();
        defaultCat.setDefaultCategory(true);

        try (MockedStatic<AuthUtils> auth = mockStatic(AuthUtils.class)) {
            auth.when(AuthUtils::getCurrentUserId).thenReturn(1L);
            when(categoryRepository.findByPublicId(categoryPublicId))
                    .thenReturn(Optional.of(defaultCat));

            assertThatThrownBy(() -> categoryService.deleteCategory(request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Cannot delete default categories");
        }
    }

    @Test
    void deleteCategory_categoryOwnedByOtherUser_throwsForbiddenException() {
        Long currentUserId = 1L;
        UUID categoryPublicId = UUID.randomUUID();

        User otherUser = userWithId(2L);
        Category category = customCategoryOwnedBy(10L, otherUser);

        DeleteCategoryRequest request = new DeleteCategoryRequest();
        request.setCategoryId(categoryPublicId);

        try (MockedStatic<AuthUtils> auth = mockStatic(AuthUtils.class)) {
            auth.when(AuthUtils::getCurrentUserId).thenReturn(currentUserId);
            when(categoryRepository.findByPublicId(categoryPublicId))
                    .thenReturn(Optional.of(category));

            assertThatThrownBy(() -> categoryService.deleteCategory(request))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("permission");
        }
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private User userWithId(Long id) {
        User user = new User();
        user.setId(id);
        return user;
    }

    private Category customCategoryOwnedBy(Long id, User owner) {
        Category category = new Category();
        category.setId(id);
        category.setDefaultCategory(false);
        category.setUser(owner);
        return category;
    }
}
