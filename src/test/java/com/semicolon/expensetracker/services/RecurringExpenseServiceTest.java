package com.semicolon.expensetracker.services;

import com.semicolon.expensetracker.data.models.Category;
import com.semicolon.expensetracker.data.models.RecurringExpenses;
import com.semicolon.expensetracker.data.models.User;
import com.semicolon.expensetracker.data.models.Wallet;
import com.semicolon.expensetracker.data.models.enums.Frequency;
import com.semicolon.expensetracker.data.repositories.CategoryRepository;
import com.semicolon.expensetracker.data.repositories.RecurringExpensesRepository;
import com.semicolon.expensetracker.data.repositories.WalletRepository;
import com.semicolon.expensetracker.dtos.request.AddRecurringExpenseRequest;
import com.semicolon.expensetracker.dtos.response.RecurringExpenseResponse;
import com.semicolon.expensetracker.exceptions.ForbiddenException;
import com.semicolon.expensetracker.exceptions.ResourceNotFoundException;
import com.semicolon.expensetracker.utils.AuthUtils;
import com.semicolon.expensetracker.utils.mappers.RecurringExpenseMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecurringExpenseServiceTest {

    @Mock
    private RecurringExpensesRepository recurringExpensesRepository;
    @Mock
    private WalletRepository walletRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private RecurringExpenseMapper recurringExpenseMapper;

    @InjectMocks
    private RecurringExpenseService recurringExpenseService;

    // ─── createRecurringExpense ───────────────────────────────────────────────

    @Test
    void createRecurringExpense_validRequest_savesAndReturnsResponse() {
        User currentUser = userWithId(1L);
        UUID walletPublicId = UUID.randomUUID();
        UUID categoryPublicId = UUID.randomUUID();

        AddRecurringExpenseRequest request = new AddRecurringExpenseRequest();
        request.setWalletId(walletPublicId);
        request.setCategoryId(categoryPublicId);
        request.setAmount(new BigDecimal("5000"));
        request.setFrequency(Frequency.MONTHLY);
        request.setNextDueDate(LocalDate.of(2026, 2, 1));

        Wallet wallet = walletWithOwner(10L, currentUser);
        Category category = categoryWithId(20L);
        RecurringExpenses savedRecurring = new RecurringExpenses();
        RecurringExpenseResponse expectedResponse = new RecurringExpenseResponse();

        try (MockedStatic<AuthUtils> auth = mockStatic(AuthUtils.class)) {
            auth.when(AuthUtils::getCurrentUser).thenReturn(currentUser);
            when(walletRepository.findByPublicId(walletPublicId))
                    .thenReturn(Optional.of(wallet));
            when(categoryRepository.findByPublicId(categoryPublicId))
                    .thenReturn(Optional.of(category));
            when(recurringExpensesRepository.save(any(RecurringExpenses.class)))
                    .thenReturn(savedRecurring);
            when(recurringExpenseMapper.toResponse(savedRecurring)).thenReturn(expectedResponse);

            RecurringExpenseResponse result =
                    recurringExpenseService.createRecurringExpense(request);

            assertThat(result).isEqualTo(expectedResponse);

            ArgumentCaptor<RecurringExpenses> captor =
                    ArgumentCaptor.forClass(RecurringExpenses.class);
            verify(recurringExpensesRepository).save(captor.capture());
            RecurringExpenses captured = captor.getValue();
            assertThat(captured.getAmount()).isEqualByComparingTo(new BigDecimal("5000"));
            assertThat(captured.getFrequency()).isEqualTo(Frequency.MONTHLY);
            assertThat(captured.getNextDueDate()).isEqualTo(LocalDate.of(2026, 2, 1));
            assertThat(captured.getWallet()).isEqualTo(wallet);
            assertThat(captured.getCategory()).isEqualTo(category);
            assertThat(captured.getUser()).isEqualTo(currentUser);
        }
    }

    @Test
    void createRecurringExpense_nullNextDueDate_defaultsToToday() {
        User currentUser = userWithId(1L);
        UUID walletPublicId = UUID.randomUUID();
        UUID categoryPublicId = UUID.randomUUID();

        AddRecurringExpenseRequest request = new AddRecurringExpenseRequest();
        request.setWalletId(walletPublicId);
        request.setCategoryId(categoryPublicId);
        request.setAmount(new BigDecimal("1000"));
        request.setFrequency(Frequency.WEEKLY);
        request.setNextDueDate(null);

        Wallet wallet = walletWithOwner(10L, currentUser);
        Category category = categoryWithId(20L);
        RecurringExpenses savedRecurring = new RecurringExpenses();

        try (MockedStatic<AuthUtils> auth = mockStatic(AuthUtils.class)) {
            auth.when(AuthUtils::getCurrentUser).thenReturn(currentUser);
            when(walletRepository.findByPublicId(walletPublicId))
                    .thenReturn(Optional.of(wallet));
            when(categoryRepository.findByPublicId(categoryPublicId))
                    .thenReturn(Optional.of(category));

            ArgumentCaptor<RecurringExpenses> captor =
                    ArgumentCaptor.forClass(RecurringExpenses.class);
            when(recurringExpensesRepository.save(captor.capture())).thenReturn(savedRecurring);
            when(recurringExpenseMapper.toResponse(savedRecurring))
                    .thenReturn(new RecurringExpenseResponse());

            recurringExpenseService.createRecurringExpense(request);

            assertThat(captor.getValue().getNextDueDate()).isNotNull();
        }
    }

    @Test
    void createRecurringExpense_walletNotFound_throwsResourceNotFoundException() {
        User currentUser = userWithId(1L);
        UUID walletPublicId = UUID.randomUUID();

        AddRecurringExpenseRequest request = new AddRecurringExpenseRequest();
        request.setWalletId(walletPublicId);
        request.setCategoryId(UUID.randomUUID());

        try (MockedStatic<AuthUtils> auth = mockStatic(AuthUtils.class)) {
            auth.when(AuthUtils::getCurrentUser).thenReturn(currentUser);
            when(walletRepository.findByPublicId(walletPublicId)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    recurringExpenseService.createRecurringExpense(request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Wallet does not exist");
        }
    }

    @Test
    void createRecurringExpense_walletBelongsToOtherUser_throwsForbiddenException() {
        User currentUser = userWithId(1L);
        User otherUser = userWithId(2L);
        UUID walletPublicId = UUID.randomUUID();

        Wallet wallet = walletWithOwner(10L, otherUser);

        AddRecurringExpenseRequest request = new AddRecurringExpenseRequest();
        request.setWalletId(walletPublicId);
        request.setCategoryId(UUID.randomUUID());

        try (MockedStatic<AuthUtils> auth = mockStatic(AuthUtils.class)) {
            auth.when(AuthUtils::getCurrentUser).thenReturn(currentUser);
            when(walletRepository.findByPublicId(walletPublicId)).thenReturn(Optional.of(wallet));

            assertThatThrownBy(() ->
                    recurringExpenseService.createRecurringExpense(request))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessage("Wallet does not belong to this user");
        }
    }

    @Test
    void createRecurringExpense_categoryNotFound_throwsResourceNotFoundException() {
        User currentUser = userWithId(1L);
        UUID walletPublicId = UUID.randomUUID();
        UUID categoryPublicId = UUID.randomUUID();

        Wallet wallet = walletWithOwner(10L, currentUser);

        AddRecurringExpenseRequest request = new AddRecurringExpenseRequest();
        request.setWalletId(walletPublicId);
        request.setCategoryId(categoryPublicId);

        try (MockedStatic<AuthUtils> auth = mockStatic(AuthUtils.class)) {
            auth.when(AuthUtils::getCurrentUser).thenReturn(currentUser);
            when(walletRepository.findByPublicId(walletPublicId)).thenReturn(Optional.of(wallet));
            when(categoryRepository.findByPublicId(categoryPublicId)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    recurringExpenseService.createRecurringExpense(request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Category does not exist");
        }
    }

    // ─── getRecurringExpensesByUser ───────────────────────────────────────────

    @Test
    void getRecurringExpensesByUser_returnsMappedExpenses() {
        Long userId = 1L;
        RecurringExpenses r1 = new RecurringExpenses();
        RecurringExpenses r2 = new RecurringExpenses();
        RecurringExpenseResponse resp1 = new RecurringExpenseResponse();
        RecurringExpenseResponse resp2 = new RecurringExpenseResponse();

        try (MockedStatic<AuthUtils> auth = mockStatic(AuthUtils.class)) {
            auth.when(AuthUtils::getCurrentUserId).thenReturn(userId);
            when(recurringExpensesRepository.findByUserId(userId)).thenReturn(List.of(r1, r2));
            when(recurringExpenseMapper.toResponse(r1)).thenReturn(resp1);
            when(recurringExpenseMapper.toResponse(r2)).thenReturn(resp2);

            List<RecurringExpenseResponse> result =
                    recurringExpenseService.getRecurringExpensesByUser();

            assertThat(result).containsExactly(resp1, resp2);
        }
    }

    @Test
    void getRecurringExpensesByUser_noExpenses_returnsEmptyList() {
        try (MockedStatic<AuthUtils> auth = mockStatic(AuthUtils.class)) {
            auth.when(AuthUtils::getCurrentUserId).thenReturn(1L);
            when(recurringExpensesRepository.findByUserId(1L)).thenReturn(List.of());

            List<RecurringExpenseResponse> result =
                    recurringExpenseService.getRecurringExpensesByUser();

            assertThat(result).isEmpty();
        }
    }

    // ─── deleteRecurringExpense ───────────────────────────────────────────────

    @Test
    void deleteRecurringExpense_ownedExpense_deletesSuccessfully() {
        Long userId = 1L;
        UUID publicId = UUID.randomUUID();

        User owner = userWithId(userId);
        RecurringExpenses expense = recurringExpenseForUser(owner);

        try (MockedStatic<AuthUtils> auth = mockStatic(AuthUtils.class)) {
            auth.when(AuthUtils::getCurrentUserId).thenReturn(userId);
            when(recurringExpensesRepository.findByPublicId(publicId))
                    .thenReturn(Optional.of(expense));

            recurringExpenseService.deleteRecurringExpense(publicId);

            verify(recurringExpensesRepository).delete(expense);
        }
    }

    @Test
    void deleteRecurringExpense_expenseNotFound_throwsResourceNotFoundException() {
        UUID publicId = UUID.randomUUID();

        try (MockedStatic<AuthUtils> auth = mockStatic(AuthUtils.class)) {
            auth.when(AuthUtils::getCurrentUserId).thenReturn(1L);
            when(recurringExpensesRepository.findByPublicId(publicId)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    recurringExpenseService.deleteRecurringExpense(publicId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Recurring expense does not exist");
        }
    }

    @Test
    void deleteRecurringExpense_expenseBelongsToOtherUser_throwsForbiddenException() {
        Long currentUserId = 1L;
        UUID publicId = UUID.randomUUID();

        User otherUser = userWithId(2L);
        RecurringExpenses expense = recurringExpenseForUser(otherUser);

        try (MockedStatic<AuthUtils> auth = mockStatic(AuthUtils.class)) {
            auth.when(AuthUtils::getCurrentUserId).thenReturn(currentUserId);
            when(recurringExpensesRepository.findByPublicId(publicId))
                    .thenReturn(Optional.of(expense));

            assertThatThrownBy(() ->
                    recurringExpenseService.deleteRecurringExpense(publicId))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessage("Recurring expense does not belong to this user");
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

    private Category categoryWithId(Long id) {
        Category category = new Category();
        category.setId(id);
        return category;
    }

    private RecurringExpenses recurringExpenseForUser(User user) {
        RecurringExpenses expense = new RecurringExpenses();
        expense.setUser(user);
        return expense;
    }
}
