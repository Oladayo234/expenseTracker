package com.semicolon.expensetracker.services;

import com.semicolon.expensetracker.data.models.Budget;
import com.semicolon.expensetracker.data.models.Category;
import com.semicolon.expensetracker.data.models.User;
import com.semicolon.expensetracker.data.repositories.BudgetRepository;
import com.semicolon.expensetracker.data.repositories.CategoryRepository;
import com.semicolon.expensetracker.data.repositories.ExpenseRepository;
import com.semicolon.expensetracker.dtos.request.CreateBudgetRequest;
import com.semicolon.expensetracker.dtos.response.BudgetLimitVsActualExpenseResponse;
import com.semicolon.expensetracker.dtos.response.BudgetResponse;
import com.semicolon.expensetracker.exceptions.ForbiddenException;
import com.semicolon.expensetracker.exceptions.InvalidEntryException;
import com.semicolon.expensetracker.exceptions.ResourceNotFoundException;
import com.semicolon.expensetracker.utils.AuthUtils;
import com.semicolon.expensetracker.utils.mappers.BudgetMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BudgetServiceTest {

    @Mock
    private BudgetRepository budgetRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private ExpenseRepository expenseRepository;
    @Mock
    private BudgetMapper budgetMapper;

    @InjectMocks
    private BudgetService budgetService;

    // ─── createBudget ─────────────────────────────────────────────────────────

    @Test
    void createBudget_newBudgetForCategoryAndMonth_savesAndReturnsResponse() {
        User currentUser = userWithId(1L);
        UUID categoryPublicId = UUID.randomUUID();
        YearMonth yearMonth = YearMonth.of(2026, 1);

        CreateBudgetRequest request = new CreateBudgetRequest();
        request.setCategoryId(categoryPublicId);
        request.setBudgetName("January Food Budget");
        request.setBudgetAmount(new BigDecimal("50000"));
        request.setYearMonth(yearMonth);

        Category category = categoryWithId(10L);

        Budget savedBudget = budgetForUserAndCategory(1L, currentUser, category, yearMonth,
                new BigDecimal("50000"));

        BudgetResponse expectedResponse = new BudgetResponse();

        try (MockedStatic<AuthUtils> auth = mockStatic(AuthUtils.class)) {
            auth.when(AuthUtils::getCurrentUser).thenReturn(currentUser);
            when(categoryRepository.findByPublicId(categoryPublicId))
                    .thenReturn(Optional.of(category));
            when(budgetRepository.existsByUserIdAndCategoryIdAndMonthYear(1L, 10L, yearMonth))
                    .thenReturn(false);
            when(budgetRepository.save(any(Budget.class))).thenReturn(savedBudget);
            when(expenseRepository.sumByCategoryAndUserAndDateRange(eq(1L), eq(10L), any(), any()))
                    .thenReturn(Optional.of(BigDecimal.ZERO));
            when(budgetMapper.toResponse(eq(savedBudget), eq(BigDecimal.ZERO),
                    eq("Budget created successfully"))).thenReturn(expectedResponse);

            BudgetResponse result = budgetService.createBudget(request);

            assertThat(result).isEqualTo(expectedResponse);

            ArgumentCaptor<Budget> captor = ArgumentCaptor.forClass(Budget.class);
            verify(budgetRepository).save(captor.capture());
            Budget captured = captor.getValue();
            assertThat(captured.getBudgetName()).isEqualTo("January Food Budget");
            assertThat(captured.getBudgetAmount()).isEqualByComparingTo(new BigDecimal("50000"));
            assertThat(captured.getCategory()).isEqualTo(category);
            assertThat(captured.getUser()).isEqualTo(currentUser);
        }
    }

    @Test
    void createBudget_categoryNotFound_throwsResourceNotFoundException() {
        User currentUser = userWithId(1L);
        UUID categoryPublicId = UUID.randomUUID();

        CreateBudgetRequest request = new CreateBudgetRequest();
        request.setCategoryId(categoryPublicId);
        request.setBudgetAmount(new BigDecimal("10000"));
        request.setYearMonth(YearMonth.of(2026, 1));

        try (MockedStatic<AuthUtils> auth = mockStatic(AuthUtils.class)) {
            auth.when(AuthUtils::getCurrentUser).thenReturn(currentUser);
            when(categoryRepository.findByPublicId(categoryPublicId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> budgetService.createBudget(request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Category does not exist");
        }
    }

    @Test
    void createBudget_duplicateBudgetForSameCategoryAndMonth_throwsInvalidEntryException() {
        User currentUser = userWithId(1L);
        UUID categoryPublicId = UUID.randomUUID();
        YearMonth yearMonth = YearMonth.of(2026, 1);

        CreateBudgetRequest request = new CreateBudgetRequest();
        request.setCategoryId(categoryPublicId);
        request.setBudgetAmount(new BigDecimal("10000"));
        request.setYearMonth(yearMonth);

        Category category = categoryWithId(10L);

        try (MockedStatic<AuthUtils> auth = mockStatic(AuthUtils.class)) {
            auth.when(AuthUtils::getCurrentUser).thenReturn(currentUser);
            when(categoryRepository.findByPublicId(categoryPublicId))
                    .thenReturn(Optional.of(category));
            when(budgetRepository.existsByUserIdAndCategoryIdAndMonthYear(1L, 10L, yearMonth))
                    .thenReturn(true);

            assertThatThrownBy(() -> budgetService.createBudget(request))
                    .isInstanceOf(InvalidEntryException.class)
                    .hasMessage("Budget already exists for this category and month");
        }
    }

    // ─── getBudgetsByUser ─────────────────────────────────────────────────────

    @Test
    void getBudgetsByUser_returnsMappedBudgetsWithActualSpent() {
        Long userId = 1L;
        User user = userWithId(userId);
        Category category = categoryWithId(10L);
        YearMonth ym = YearMonth.of(2026, 1);

        Budget b1 = budgetForUserAndCategory(1L, user, category, ym, new BigDecimal("20000"));
        Budget b2 = budgetForUserAndCategory(2L, user, category, ym, new BigDecimal("30000"));

        BudgetResponse r1 = new BudgetResponse();
        BudgetResponse r2 = new BudgetResponse();

        try (MockedStatic<AuthUtils> auth = mockStatic(AuthUtils.class)) {
            auth.when(AuthUtils::getCurrentUserId).thenReturn(userId);
            when(budgetRepository.findByUserId(userId)).thenReturn(List.of(b1, b2));
            when(expenseRepository.sumByCategoryAndUserAndDateRange(eq(userId), eq(10L), any(), any()))
                    .thenReturn(Optional.of(new BigDecimal("5000")));
            when(budgetMapper.toResponse(eq(b1), eq(new BigDecimal("5000")), eq("Success")))
                    .thenReturn(r1);
            when(budgetMapper.toResponse(eq(b2), eq(new BigDecimal("5000")), eq("Success")))
                    .thenReturn(r2);

            List<BudgetResponse> result = budgetService.getBudgetsByUser();

            assertThat(result).containsExactly(r1, r2);
        }
    }

    @Test
    void getBudgetsByUser_noBudgets_returnsEmptyList() {
        try (MockedStatic<AuthUtils> auth = mockStatic(AuthUtils.class)) {
            auth.when(AuthUtils::getCurrentUserId).thenReturn(1L);
            when(budgetRepository.findByUserId(1L)).thenReturn(List.of());

            List<BudgetResponse> result = budgetService.getBudgetsByUser();

            assertThat(result).isEmpty();
        }
    }

    // ─── getBudgetVsActual ────────────────────────────────────────────────────

    @Test
    void getBudgetVsActual_returnsMappedVsActualResponses() {
        Long userId = 1L;
        User user = userWithId(userId);
        Category category = categoryWithId(10L);
        YearMonth ym = YearMonth.of(2026, 1);

        Budget b1 = budgetForUserAndCategory(1L, user, category, ym, new BigDecimal("10000"));
        BudgetLimitVsActualExpenseResponse vsResponse = BudgetLimitVsActualExpenseResponse.builder()
                .status("ON_TRACK").build();

        try (MockedStatic<AuthUtils> auth = mockStatic(AuthUtils.class)) {
            auth.when(AuthUtils::getCurrentUserId).thenReturn(userId);
            when(budgetRepository.findByUserId(userId)).thenReturn(List.of(b1));
            when(expenseRepository.sumByCategoryAndUserAndDateRange(eq(userId), eq(10L), any(), any()))
                    .thenReturn(Optional.of(new BigDecimal("3000")));
            when(budgetMapper.toVsActualResponse(b1, new BigDecimal("3000"))).thenReturn(vsResponse);

            List<BudgetLimitVsActualExpenseResponse> result = budgetService.getBudgetVsActual();

            assertThat(result).containsExactly(vsResponse);
        }
    }

    @Test
    void getBudgetVsActual_noActualSpent_usesZero() {
        Long userId = 1L;
        User user = userWithId(userId);
        Category category = categoryWithId(10L);
        YearMonth ym = YearMonth.of(2026, 2);

        Budget b1 = budgetForUserAndCategory(1L, user, category, ym, new BigDecimal("10000"));
        BudgetLimitVsActualExpenseResponse vsResponse = BudgetLimitVsActualExpenseResponse.builder()
                .status("ON_TRACK").build();

        try (MockedStatic<AuthUtils> auth = mockStatic(AuthUtils.class)) {
            auth.when(AuthUtils::getCurrentUserId).thenReturn(userId);
            when(budgetRepository.findByUserId(userId)).thenReturn(List.of(b1));
            when(expenseRepository.sumByCategoryAndUserAndDateRange(eq(userId), eq(10L), any(), any()))
                    .thenReturn(Optional.empty());
            when(budgetMapper.toVsActualResponse(b1, BigDecimal.ZERO)).thenReturn(vsResponse);

            List<BudgetLimitVsActualExpenseResponse> result = budgetService.getBudgetVsActual();

            assertThat(result).containsExactly(vsResponse);
        }
    }

    // ─── deleteBudget ─────────────────────────────────────────────────────────

    @Test
    void deleteBudget_ownedBudget_deletesSuccessfully() {
        Long userId = 1L;
        UUID publicId = UUID.randomUUID();

        User user = userWithId(userId);
        Category category = categoryWithId(10L);
        Budget budget = budgetForUserAndCategory(1L, user, category,
                YearMonth.of(2026, 1), new BigDecimal("5000"));

        try (MockedStatic<AuthUtils> auth = mockStatic(AuthUtils.class)) {
            auth.when(AuthUtils::getCurrentUserId).thenReturn(userId);
            when(budgetRepository.findByPublicId(publicId)).thenReturn(Optional.of(budget));

            budgetService.deleteBudget(publicId);

            verify(budgetRepository).delete(budget);
        }
    }

    @Test
    void deleteBudget_budgetNotFound_throwsResourceNotFoundException() {
        UUID publicId = UUID.randomUUID();

        try (MockedStatic<AuthUtils> auth = mockStatic(AuthUtils.class)) {
            auth.when(AuthUtils::getCurrentUserId).thenReturn(1L);
            when(budgetRepository.findByPublicId(publicId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> budgetService.deleteBudget(publicId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Budget does not exist");
        }
    }

    @Test
    void deleteBudget_budgetBelongsToOtherUser_throwsForbiddenException() {
        Long currentUserId = 1L;
        UUID publicId = UUID.randomUUID();

        User otherUser = userWithId(2L);
        Category category = categoryWithId(10L);
        Budget budget = budgetForUserAndCategory(1L, otherUser, category,
                YearMonth.of(2026, 1), new BigDecimal("5000"));

        try (MockedStatic<AuthUtils> auth = mockStatic(AuthUtils.class)) {
            auth.when(AuthUtils::getCurrentUserId).thenReturn(currentUserId);
            when(budgetRepository.findByPublicId(publicId)).thenReturn(Optional.of(budget));

            assertThatThrownBy(() -> budgetService.deleteBudget(publicId))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessage("Budget does not belong to this user");
        }
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private User userWithId(Long id) {
        User user = new User();
        user.setId(id);
        return user;
    }

    private Category categoryWithId(Long id) {
        Category category = new Category();
        category.setId(id);
        return category;
    }

    private Budget budgetForUserAndCategory(Long budgetId, User user, Category category,
                                            YearMonth yearMonth, BigDecimal amount) {
        Budget budget = new Budget();
        budget.setId(budgetId);
        budget.setUser(user);
        budget.setCategory(category);
        budget.setMonthYear(yearMonth);
        budget.setBudgetAmount(amount);
        return budget;
    }
}
