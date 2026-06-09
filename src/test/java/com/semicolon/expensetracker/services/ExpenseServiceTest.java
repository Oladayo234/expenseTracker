package com.semicolon.expensetracker.services;

import com.semicolon.expensetracker.data.models.Category;
import com.semicolon.expensetracker.data.models.Expense;
import com.semicolon.expensetracker.data.models.User;
import com.semicolon.expensetracker.data.models.Wallet;
import com.semicolon.expensetracker.data.models.enums.PaymentMethod;
import com.semicolon.expensetracker.data.models.enums.TransactionType;
import com.semicolon.expensetracker.data.repositories.CategoryRepository;
import com.semicolon.expensetracker.data.repositories.ExpenseRepository;
import com.semicolon.expensetracker.data.repositories.WalletRepository;
import com.semicolon.expensetracker.dtos.request.AddExpenseRequest;
import com.semicolon.expensetracker.dtos.response.AddExpenseResponse;
import com.semicolon.expensetracker.dtos.response.CategoryBreakdownResponse;
import com.semicolon.expensetracker.dtos.response.MonthlySummaryResponse;
import com.semicolon.expensetracker.exceptions.ForbiddenException;
import com.semicolon.expensetracker.exceptions.ResourceNotFoundException;
import com.semicolon.expensetracker.utils.AuthUtils;
import com.semicolon.expensetracker.utils.mappers.ExpenseMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExpenseServiceTest {

    @Mock
    private ExpenseRepository expenseRepository;
    @Mock
    private WalletRepository walletRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private ExpenseMapper expenseMapper;

    @InjectMocks
    private ExpenseService expenseService;

    // ─── addExpense ───────────────────────────────────────────────────────────

    @Test
    void addExpense_validRequest_savesAndReturnsResponse() {
        Long userId = 1L;
        UUID walletPublicId = UUID.randomUUID();
        UUID categoryPublicId = UUID.randomUUID();

        AddExpenseRequest request = new AddExpenseRequest();
        request.setWalletId(walletPublicId);
        request.setCategoryId(categoryPublicId);
        request.setAmount(new BigDecimal("100.00"));
        request.setNote("Lunch");
        request.setPaymentMethod(PaymentMethod.CASH);
        request.setExpenseDate(LocalDateTime.of(2026, 1, 15, 12, 0));

        User owner = userWithId(userId);
        Wallet wallet = walletWithOwner(10L, owner);
        Category category = defaultCategory(20L);

        Expense savedExpense = new Expense();
        AddExpenseResponse expectedResponse = new AddExpenseResponse();

        try (MockedStatic<AuthUtils> auth = mockStatic(AuthUtils.class)) {
            auth.when(AuthUtils::getCurrentUserId).thenReturn(userId);
            when(walletRepository.findByPublicId(walletPublicId)).thenReturn(Optional.of(wallet));
            when(categoryRepository.findByPublicId(categoryPublicId)).thenReturn(Optional.of(category));
            when(expenseRepository.save(any(Expense.class))).thenReturn(savedExpense);
            when(expenseMapper.toResponse(savedExpense)).thenReturn(expectedResponse);

            AddExpenseResponse result = expenseService.addExpense(request);

            assertThat(result).isEqualTo(expectedResponse);
        }
    }

    @Test
    void addExpense_nullExpenseDate_usesCurrentTime() {
        Long userId = 1L;
        UUID walletPublicId = UUID.randomUUID();
        UUID categoryPublicId = UUID.randomUUID();

        AddExpenseRequest request = new AddExpenseRequest();
        request.setWalletId(walletPublicId);
        request.setCategoryId(categoryPublicId);
        request.setAmount(new BigDecimal("50.00"));
        request.setPaymentMethod(PaymentMethod.TRANSFER);
        request.setExpenseDate(null);

        User owner = userWithId(userId);
        Wallet wallet = walletWithOwner(10L, owner);
        Category category = defaultCategory(20L);
        Expense savedExpense = new Expense();

        try (MockedStatic<AuthUtils> auth = mockStatic(AuthUtils.class)) {
            auth.when(AuthUtils::getCurrentUserId).thenReturn(userId);
            when(walletRepository.findByPublicId(walletPublicId)).thenReturn(Optional.of(wallet));
            when(categoryRepository.findByPublicId(categoryPublicId)).thenReturn(Optional.of(category));

            ArgumentCaptor<Expense> captor = ArgumentCaptor.forClass(Expense.class);
            when(expenseRepository.save(captor.capture())).thenReturn(savedExpense);
            when(expenseMapper.toResponse(savedExpense)).thenReturn(new AddExpenseResponse());

            expenseService.addExpense(request);

            assertThat(captor.getValue().getExpenseDate()).isNotNull();
        }
    }

    @Test
    void addExpense_walletNotFound_throwsResourceNotFoundException() {
        UUID walletPublicId = UUID.randomUUID();
        AddExpenseRequest request = new AddExpenseRequest();
        request.setWalletId(walletPublicId);
        request.setCategoryId(UUID.randomUUID());

        try (MockedStatic<AuthUtils> auth = mockStatic(AuthUtils.class)) {
            auth.when(AuthUtils::getCurrentUserId).thenReturn(1L);
            when(walletRepository.findByPublicId(walletPublicId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> expenseService.addExpense(request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Wallet does not exist");
        }
    }

    @Test
    void addExpense_walletOwnedByOtherUser_throwsForbiddenException() {
        Long currentUserId = 1L;
        UUID walletPublicId = UUID.randomUUID();

        User anotherOwner = userWithId(2L);
        Wallet wallet = walletWithOwner(10L, anotherOwner);

        AddExpenseRequest request = new AddExpenseRequest();
        request.setWalletId(walletPublicId);
        request.setCategoryId(UUID.randomUUID());

        try (MockedStatic<AuthUtils> auth = mockStatic(AuthUtils.class)) {
            auth.when(AuthUtils::getCurrentUserId).thenReturn(currentUserId);
            when(walletRepository.findByPublicId(walletPublicId)).thenReturn(Optional.of(wallet));

            assertThatThrownBy(() -> expenseService.addExpense(request))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessage("Wallet does not belong to this user");
        }
    }

    @Test
    void addExpense_categoryNotFound_throwsResourceNotFoundException() {
        Long userId = 1L;
        UUID walletPublicId = UUID.randomUUID();
        UUID categoryPublicId = UUID.randomUUID();

        User owner = userWithId(userId);
        Wallet wallet = walletWithOwner(10L, owner);

        AddExpenseRequest request = new AddExpenseRequest();
        request.setWalletId(walletPublicId);
        request.setCategoryId(categoryPublicId);

        try (MockedStatic<AuthUtils> auth = mockStatic(AuthUtils.class)) {
            auth.when(AuthUtils::getCurrentUserId).thenReturn(userId);
            when(walletRepository.findByPublicId(walletPublicId)).thenReturn(Optional.of(wallet));
            when(categoryRepository.findByPublicId(categoryPublicId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> expenseService.addExpense(request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Category does not exist");
        }
    }

    @Test
    void addExpense_customCategoryOwnedByOtherUser_throwsForbiddenException() {
        Long userId = 1L;
        UUID walletPublicId = UUID.randomUUID();
        UUID categoryPublicId = UUID.randomUUID();

        User owner = userWithId(userId);
        Wallet wallet = walletWithOwner(10L, owner);

        User otherUser = userWithId(2L);
        Category category = customCategoryOwnedBy(20L, otherUser);

        AddExpenseRequest request = new AddExpenseRequest();
        request.setWalletId(walletPublicId);
        request.setCategoryId(categoryPublicId);

        try (MockedStatic<AuthUtils> auth = mockStatic(AuthUtils.class)) {
            auth.when(AuthUtils::getCurrentUserId).thenReturn(userId);
            when(walletRepository.findByPublicId(walletPublicId)).thenReturn(Optional.of(wallet));
            when(categoryRepository.findByPublicId(categoryPublicId)).thenReturn(Optional.of(category));

            assertThatThrownBy(() -> expenseService.addExpense(request))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessage("Category does not belong to this user");
        }
    }

    // ─── deleteExpense ────────────────────────────────────────────────────────

    @Test
    void deleteExpense_ownedExpense_deletesSuccessfully() {
        Long userId = 1L;
        UUID publicId = UUID.randomUUID();

        User owner = userWithId(userId);
        Wallet wallet = walletWithOwner(10L, owner);
        Expense expense = expenseWithWallet(wallet);

        try (MockedStatic<AuthUtils> auth = mockStatic(AuthUtils.class)) {
            auth.when(AuthUtils::getCurrentUserId).thenReturn(userId);
            when(expenseRepository.findByPublicId(publicId)).thenReturn(Optional.of(expense));

            expenseService.deleteExpense(publicId);

            verify(expenseRepository).delete(expense);
        }
    }

    @Test
    void deleteExpense_expenseNotFound_throwsResourceNotFoundException() {
        UUID publicId = UUID.randomUUID();

        try (MockedStatic<AuthUtils> auth = mockStatic(AuthUtils.class)) {
            auth.when(AuthUtils::getCurrentUserId).thenReturn(1L);
            when(expenseRepository.findByPublicId(publicId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> expenseService.deleteExpense(publicId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Expense does not exist");
        }
    }

    @Test
    void deleteExpense_expenseBelongsToOtherUser_throwsForbiddenException() {
        Long currentUserId = 1L;
        UUID publicId = UUID.randomUUID();

        User otherUser = userWithId(2L);
        Wallet wallet = walletWithOwner(10L, otherUser);
        Expense expense = expenseWithWallet(wallet);

        try (MockedStatic<AuthUtils> auth = mockStatic(AuthUtils.class)) {
            auth.when(AuthUtils::getCurrentUserId).thenReturn(currentUserId);
            when(expenseRepository.findByPublicId(publicId)).thenReturn(Optional.of(expense));

            assertThatThrownBy(() -> expenseService.deleteExpense(publicId))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessage("Expense does not belong to this user");
        }
    }

    // ─── getExpensesByWallet ──────────────────────────────────────────────────

    @Test
    void getExpensesByWallet_ownedWallet_returnsMappedExpenses() {
        Long userId = 1L;
        UUID walletPublicId = UUID.randomUUID();

        User owner = userWithId(userId);
        Wallet wallet = walletWithOwner(10L, owner);
        Expense e1 = new Expense();
        Expense e2 = new Expense();
        AddExpenseResponse r1 = new AddExpenseResponse();
        AddExpenseResponse r2 = new AddExpenseResponse();

        try (MockedStatic<AuthUtils> auth = mockStatic(AuthUtils.class)) {
            auth.when(AuthUtils::getCurrentUserId).thenReturn(userId);
            when(walletRepository.findByPublicId(walletPublicId)).thenReturn(Optional.of(wallet));
            when(expenseRepository.findByWalletIdOrderByExpenseDateDesc(10L))
                    .thenReturn(List.of(e1, e2));
            when(expenseMapper.toResponse(e1)).thenReturn(r1);
            when(expenseMapper.toResponse(e2)).thenReturn(r2);

            List<AddExpenseResponse> result = expenseService.getExpensesByWallet(walletPublicId);

            assertThat(result).containsExactly(r1, r2);
        }
    }

    @Test
    void getExpensesByWallet_walletNotFound_throwsResourceNotFoundException() {
        UUID walletPublicId = UUID.randomUUID();

        try (MockedStatic<AuthUtils> auth = mockStatic(AuthUtils.class)) {
            auth.when(AuthUtils::getCurrentUserId).thenReturn(1L);
            when(walletRepository.findByPublicId(walletPublicId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> expenseService.getExpensesByWallet(walletPublicId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Wallet does not exist");
        }
    }

    @Test
    void getExpensesByWallet_walletOwnedByOtherUser_throwsForbiddenException() {
        Long currentUserId = 1L;
        UUID walletPublicId = UUID.randomUUID();

        User otherUser = userWithId(2L);
        Wallet wallet = walletWithOwner(10L, otherUser);

        try (MockedStatic<AuthUtils> auth = mockStatic(AuthUtils.class)) {
            auth.when(AuthUtils::getCurrentUserId).thenReturn(currentUserId);
            when(walletRepository.findByPublicId(walletPublicId)).thenReturn(Optional.of(wallet));

            assertThatThrownBy(() -> expenseService.getExpensesByWallet(walletPublicId))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessage("Wallet does not belong to this user");
        }
    }

    // ─── getExpensesByCategory ────────────────────────────────────────────────

    @Test
    void getExpensesByCategory_accessibleCategory_returnsMappedExpenses() {
        Long userId = 1L;
        UUID categoryPublicId = UUID.randomUUID();

        Category category = defaultCategory(20L);
        Expense e1 = new Expense();
        AddExpenseResponse r1 = new AddExpenseResponse();

        try (MockedStatic<AuthUtils> auth = mockStatic(AuthUtils.class)) {
            auth.when(AuthUtils::getCurrentUserId).thenReturn(userId);
            when(categoryRepository.findByPublicId(categoryPublicId)).thenReturn(Optional.of(category));
            when(expenseRepository.findByCategoryIdOrderByExpenseDateDesc(20L))
                    .thenReturn(List.of(e1));
            when(expenseMapper.toResponse(e1)).thenReturn(r1);

            List<AddExpenseResponse> result = expenseService.getExpensesByCategory(categoryPublicId);

            assertThat(result).containsExactly(r1);
        }
    }

    @Test
    void getExpensesByCategory_categoryNotFound_throwsResourceNotFoundException() {
        UUID categoryPublicId = UUID.randomUUID();

        try (MockedStatic<AuthUtils> auth = mockStatic(AuthUtils.class)) {
            auth.when(AuthUtils::getCurrentUserId).thenReturn(1L);
            when(categoryRepository.findByPublicId(categoryPublicId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> expenseService.getExpensesByCategory(categoryPublicId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Category does not exist");
        }
    }

    @Test
    void getExpensesByCategory_customCategoryOwnedByOtherUser_throwsForbiddenException() {
        Long currentUserId = 1L;
        UUID categoryPublicId = UUID.randomUUID();

        User otherUser = userWithId(2L);
        Category category = customCategoryOwnedBy(20L, otherUser);

        try (MockedStatic<AuthUtils> auth = mockStatic(AuthUtils.class)) {
            auth.when(AuthUtils::getCurrentUserId).thenReturn(currentUserId);
            when(categoryRepository.findByPublicId(categoryPublicId)).thenReturn(Optional.of(category));

            assertThatThrownBy(() -> expenseService.getExpensesByCategory(categoryPublicId))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessage("Category does not belong to this user");
        }
    }

    // ─── getExpensesByDateRange ───────────────────────────────────────────────

    @Test
    void getExpensesByDateRange_ownedWallet_returnsExpensesInRange() {
        Long userId = 1L;
        UUID walletPublicId = UUID.randomUUID();
        LocalDateTime start = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 1, 31, 23, 59, 59);

        User owner = userWithId(userId);
        Wallet wallet = walletWithOwner(10L, owner);
        Expense e1 = new Expense();
        AddExpenseResponse r1 = new AddExpenseResponse();

        try (MockedStatic<AuthUtils> auth = mockStatic(AuthUtils.class)) {
            auth.when(AuthUtils::getCurrentUserId).thenReturn(userId);
            when(walletRepository.findByPublicId(walletPublicId)).thenReturn(Optional.of(wallet));
            when(expenseRepository.findByWalletIdAndExpenseDateBetweenOrderByExpenseDateDesc(
                    10L, start, end)).thenReturn(List.of(e1));
            when(expenseMapper.toResponse(e1)).thenReturn(r1);

            List<AddExpenseResponse> result = expenseService.getExpensesByDateRange(
                    walletPublicId, start, end);

            assertThat(result).containsExactly(r1);
        }
    }

    @Test
    void getExpensesByDateRange_walletNotFound_throwsResourceNotFoundException() {
        UUID walletPublicId = UUID.randomUUID();

        try (MockedStatic<AuthUtils> auth = mockStatic(AuthUtils.class)) {
            auth.when(AuthUtils::getCurrentUserId).thenReturn(1L);
            when(walletRepository.findByPublicId(walletPublicId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> expenseService.getExpensesByDateRange(
                    walletPublicId, LocalDateTime.now(), LocalDateTime.now()))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Wallet does not exist");
        }
    }

    @Test
    void getExpensesByDateRange_walletOwnedByOtherUser_throwsForbiddenException() {
        Long currentUserId = 1L;
        UUID walletPublicId = UUID.randomUUID();

        User otherUser = userWithId(2L);
        Wallet wallet = walletWithOwner(10L, otherUser);

        try (MockedStatic<AuthUtils> auth = mockStatic(AuthUtils.class)) {
            auth.when(AuthUtils::getCurrentUserId).thenReturn(currentUserId);
            when(walletRepository.findByPublicId(walletPublicId)).thenReturn(Optional.of(wallet));

            assertThatThrownBy(() -> expenseService.getExpensesByDateRange(
                    walletPublicId, LocalDateTime.now(), LocalDateTime.now()))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessage("Wallet does not belong to this user");
        }
    }

    // ─── getMonthlySummary ────────────────────────────────────────────────────

    @Test
    void getMonthlySummary_mixedTransactionTypes_computesCorrectTotals() {
        Long userId = 1L;

        Category inflowCat = categoryWithType(TransactionType.INFLOW);
        Category fixedCat = categoryWithType(TransactionType.OUTFLOW_FIXED_COST);
        Category varCat = categoryWithType(TransactionType.OUTFLOW_VARIABLE_COST);
        Category outflowCat = categoryWithType(TransactionType.OUTFLOW);

        Expense income = expenseWithAmountAndCategory(new BigDecimal("1000"), inflowCat);
        Expense fixed = expenseWithAmountAndCategory(new BigDecimal("200"), fixedCat);
        Expense variable = expenseWithAmountAndCategory(new BigDecimal("150"), varCat);
        Expense outflow = expenseWithAmountAndCategory(new BigDecimal("50"), outflowCat);

        try (MockedStatic<AuthUtils> auth = mockStatic(AuthUtils.class)) {
            auth.when(AuthUtils::getCurrentUserId).thenReturn(userId);
            when(expenseRepository.findByWalletUserIdAndExpenseDateBetween(
                    eq(userId), any(), any()))
                    .thenReturn(List.of(income, fixed, variable, outflow));

            MonthlySummaryResponse result = expenseService.getMonthlySummary(2026, 1);

            assertThat(result.getTotalIncome()).isEqualByComparingTo(new BigDecimal("1000"));
            assertThat(result.getTotalExpenses()).isEqualByComparingTo(new BigDecimal("400"));
            assertThat(result.getNetAmount()).isEqualByComparingTo(new BigDecimal("600"));
            assertThat(result.getYear()).isEqualTo(2026);
            assertThat(result.getMonth()).isEqualTo(1);
        }
    }

    @Test
    void getMonthlySummary_noExpenses_returnsAllZeros() {
        Long userId = 1L;

        try (MockedStatic<AuthUtils> auth = mockStatic(AuthUtils.class)) {
            auth.when(AuthUtils::getCurrentUserId).thenReturn(userId);
            when(expenseRepository.findByWalletUserIdAndExpenseDateBetween(
                    eq(userId), any(), any()))
                    .thenReturn(List.of());

            MonthlySummaryResponse result = expenseService.getMonthlySummary(2026, 3);

            assertThat(result.getTotalIncome()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.getTotalExpenses()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.getNetAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    @Test
    void getMonthlySummary_expenseWithNullTransactionType_isSkipped() {
        Long userId = 1L;

        Category nullTypeCat = new Category();
        nullTypeCat.setTransactionType(null);
        Expense expense = expenseWithAmountAndCategory(new BigDecimal("100"), nullTypeCat);

        try (MockedStatic<AuthUtils> auth = mockStatic(AuthUtils.class)) {
            auth.when(AuthUtils::getCurrentUserId).thenReturn(userId);
            when(expenseRepository.findByWalletUserIdAndExpenseDateBetween(
                    eq(userId), any(), any()))
                    .thenReturn(List.of(expense));

            MonthlySummaryResponse result = expenseService.getMonthlySummary(2026, 1);

            assertThat(result.getTotalIncome()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.getTotalExpenses()).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    // ─── getCategoryBreakdown ─────────────────────────────────────────────────

    @Test
    void getCategoryBreakdown_delegatesToRepository_returnsResult() {
        Long userId = 1L;
        CategoryBreakdownResponse cbr = new CategoryBreakdownResponse(
                UUID.randomUUID(), "Food", new BigDecimal("300"));

        try (MockedStatic<AuthUtils> auth = mockStatic(AuthUtils.class)) {
            auth.when(AuthUtils::getCurrentUserId).thenReturn(userId);
            when(expenseRepository.findCategoryBreakdownByUserIdAndDateRange(
                    eq(userId), any(), any()))
                    .thenReturn(List.of(cbr));

            List<CategoryBreakdownResponse> result = expenseService.getCategoryBreakdown(2026, 1);

            assertThat(result).containsExactly(cbr);
        }
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private User userWithId(Long id) {
        User user = new User();
        user.setId(id);
        return user;
    }

    private Wallet walletWithOwner(Long walletId, User owner) {
        Wallet wallet = new Wallet();
        wallet.setId(walletId);
        wallet.setUser(owner);
        return wallet;
    }

    private Expense expenseWithWallet(Wallet wallet) {
        Expense expense = new Expense();
        expense.setWallet(wallet);
        return expense;
    }

    private Category defaultCategory(Long id) {
        Category category = new Category();
        category.setId(id);
        category.setDefaultCategory(true);
        return category;
    }

    private Category customCategoryOwnedBy(Long id, User user) {
        Category category = new Category();
        category.setId(id);
        category.setDefaultCategory(false);
        category.setUser(user);
        return category;
    }

    private Category categoryWithType(TransactionType type) {
        Category category = new Category();
        category.setTransactionType(type);
        return category;
    }

    private Expense expenseWithAmountAndCategory(BigDecimal amount, Category category) {
        Expense expense = new Expense();
        expense.setAmount(amount);
        expense.setCategory(category);
        return expense;
    }
}
