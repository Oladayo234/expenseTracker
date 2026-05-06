package com.semicolon.expensetracker.services;

import com.semicolon.expensetracker.data.models.Category;
import com.semicolon.expensetracker.data.models.User;
import com.semicolon.expensetracker.data.models.Wallet;
import com.semicolon.expensetracker.data.models.enums.Currency;
import com.semicolon.expensetracker.data.models.enums.PaymentMethod;
import com.semicolon.expensetracker.data.models.enums.TransactionType;
import com.semicolon.expensetracker.data.repositories.CategoryRepository;
import com.semicolon.expensetracker.data.repositories.UserRepository;
import com.semicolon.expensetracker.data.repositories.WalletRepository;
import com.semicolon.expensetracker.dtos.request.AddExpenseRequest;
import com.semicolon.expensetracker.dtos.request.CreateBudgetRequest;
import com.semicolon.expensetracker.dtos.response.BudgetLimitVsActualExpenseResponse;
import com.semicolon.expensetracker.dtos.response.BudgetResponse;
import com.semicolon.expensetracker.exceptions.InvalidEntryException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
class BudgetServiceTest {

    @Autowired BudgetService budgetService;
    @Autowired ExpenseService expenseService;
    @Autowired UserRepository userRepository;
    @Autowired CategoryRepository categoryRepository;
    @Autowired WalletRepository walletRepository;

    private User savedUser;
    private Category savedCategory;
    private Wallet savedWallet;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setEmail("budget@test.com");
        user.setUsername("budgetuser");
        user.setName("Budget User");
        user.setPassword("hash");
        user.setPhoneNumber("05000000000");
        savedUser = userRepository.save(user);

        Category category = new Category();
        category.setName("Groceries");
        category.setTransactionType(TransactionType.OUTFLOW_VARIABLE_COST);
        category.setDefaultCategory(true);
        savedCategory = categoryRepository.save(category);

        Wallet wallet = new Wallet();
        wallet.setUser(savedUser);
        wallet.setName("Main Wallet");
        wallet.setCurrency(Currency.NAIRA);
        savedWallet = walletRepository.save(wallet);
    }

    @Test
    void createBudget_success_returnsResponseWithZeroActualSpent() {
        BudgetResponse result = budgetService.createBudget(buildBudgetRequest(YearMonth.of(2024, 5)));

        assertThat(result.getBudgetAmount()).isEqualByComparingTo("1000");
        assertThat(result.getCategoryId()).isEqualTo(savedCategory.getId());
        assertThat(result.getActualSpent()).isEqualByComparingTo("0");
        assertThat(result.getAmountRemaining()).isEqualByComparingTo("1000");
    }

    @Test
    void createBudget_duplicate_throwsInvalidEntryException() {
        YearMonth yearMonth = YearMonth.of(2024, 6);
        budgetService.createBudget(buildBudgetRequest(yearMonth));
        assertThrows(InvalidEntryException.class, () -> budgetService.createBudget(buildBudgetRequest(yearMonth)));
    }

    @Test
    void deleteBudget_notOwnedByUser_throwsInvalidEntryException() {
        User other = new User();
        other.setEmail("other@budget.com");
        other.setUsername("otherbudget");
        other.setName("Other");
        other.setPassword("hash");
        other.setPhoneNumber("04000000000");
        User savedOther = userRepository.save(other);

        BudgetResponse created = budgetService.createBudget(buildBudgetRequest(YearMonth.of(2024, 7)));

        assertThrows(InvalidEntryException.class,
                () -> budgetService.deleteBudget(created.getId(), savedOther.getId()));
    }

    @Test
    void deleteBudget_success_removesFromUserBudgets() {
        BudgetResponse created = budgetService.createBudget(buildBudgetRequest(YearMonth.of(2024, 8)));
        budgetService.deleteBudget(created.getId(), savedUser.getId());

        List<BudgetResponse> remaining = budgetService.getBudgetsByUser(savedUser.getId());
        assertThat(remaining).noneMatch(b -> b.getId().equals(created.getId()));
    }

    @Test
    void getBudgetVsActual_withNoExpenses_showsZeroSpentAndOnTrack() {
        budgetService.createBudget(buildBudgetRequest(YearMonth.of(2025, 3)));

        List<BudgetLimitVsActualExpenseResponse> results = budgetService.getBudgetVsActual(savedUser.getId());

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getActualSpent()).isEqualByComparingTo("0");
        assertThat(results.get(0).getPercentageUsed()).isEqualTo(0.0);
        assertThat(results.get(0).getStatus()).isEqualTo("ON_TRACK");
    }

    @Test
    void getBudgetVsActual_withExpenses_reflectsActualSpentCorrectly() {
        YearMonth now = YearMonth.now();
        budgetService.createBudget(buildBudgetRequest(now));

        AddExpenseRequest expense = new AddExpenseRequest();
        expense.setUserId(savedUser.getId());
        expense.setWalletId(savedWallet.getId());
        expense.setCategoryId(savedCategory.getId());
        expense.setAmount(BigDecimal.valueOf(600));
        expense.setPaymentMethod(PaymentMethod.CASH);
        expenseService.addExpense(expense);

        List<BudgetLimitVsActualExpenseResponse> results = budgetService.getBudgetVsActual(savedUser.getId());

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getActualSpent()).isEqualByComparingTo("600");
        assertThat(results.get(0).getRemaining()).isEqualByComparingTo("400");
        assertThat(results.get(0).getPercentageUsed()).isEqualTo(60.0);
        assertThat(results.get(0).getStatus()).isEqualTo("ON_TRACK");
    }

    private CreateBudgetRequest buildBudgetRequest(YearMonth yearMonth) {
        CreateBudgetRequest r = new CreateBudgetRequest();
        r.setUserId(savedUser.getId());
        r.setCategoryId(savedCategory.getId());
        r.setBudgetAmount(BigDecimal.valueOf(1000));
        r.setBudgetName("Test Budget");
        r.setYearMonth(yearMonth);
        return r;
    }
}