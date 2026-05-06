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
import com.semicolon.expensetracker.dtos.response.AddExpenseResponse;
import com.semicolon.expensetracker.dtos.response.MonthlySummaryResponse;
import com.semicolon.expensetracker.exceptions.InvalidEntryException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
class ExpenseServiceTest {

    @Autowired ExpenseService expenseService;
    @Autowired UserRepository userRepository;
    @Autowired WalletRepository walletRepository;
    @Autowired CategoryRepository categoryRepository;

    private User savedUser;
    private Wallet savedWallet;
    private Category outflowCategory;
    private Category inflowCategory;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setEmail("expense@test.com");
        user.setUsername("expenseuser");
        user.setName("Expense User");
        user.setPassword("hash");
        user.setPhoneNumber("09000000000");
        savedUser = userRepository.save(user);

        Wallet wallet = new Wallet();
        wallet.setUser(savedUser);
        wallet.setName("Test Wallet");
        wallet.setCurrency(Currency.NAIRA);
        savedWallet = walletRepository.save(wallet);

        Category outflow = new Category();
        outflow.setName("Food");
        outflow.setTransactionType(TransactionType.OUTFLOW_VARIABLE_COST);
        outflow.setDefaultCategory(true);
        outflowCategory = categoryRepository.save(outflow);

        Category inflow = new Category();
        inflow.setName("Salary");
        inflow.setTransactionType(TransactionType.INFLOW);
        inflow.setDefaultCategory(true);
        inflowCategory = categoryRepository.save(inflow);
    }

    @Test
    void addExpense_walletNotOwnedByUser_throwsInvalidEntryException() {
        User other = new User();
        other.setEmail("other@test.com");
        other.setUsername("otheruser");
        other.setName("Other");
        other.setPassword("hash");
        other.setPhoneNumber("08011111111");
        User savedOther = userRepository.save(other);

        assertThrows(InvalidEntryException.class, () ->
                expenseService.addExpense(buildRequest(savedOther.getId(), savedWallet.getId(),
                        outflowCategory.getId(), BigDecimal.valueOf(100))));
    }

    @Test
    void addExpense_categoryNotOwnedByUser_throwsInvalidEntryException() {
        User other = new User();
        other.setEmail("other2@test.com");
        other.setUsername("otheruser2");
        other.setName("Other2");
        other.setPassword("hash");
        other.setPhoneNumber("08022222222");
        User savedOther = userRepository.save(other);

        Category personalCat = new Category();
        personalCat.setName("PersonalCat");
        personalCat.setTransactionType(TransactionType.OUTFLOW_VARIABLE_COST);
        personalCat.setDefaultCategory(false);
        personalCat.setUser(savedOther);
        Category saved = categoryRepository.save(personalCat);

        assertThrows(InvalidEntryException.class, () ->
                expenseService.addExpense(buildRequest(savedUser.getId(), savedWallet.getId(),
                        saved.getId(), BigDecimal.valueOf(50))));
    }

    @Test
    void addExpense_success_returnsResponseWithCorrectFields() {
        AddExpenseResponse result = expenseService.addExpense(
                buildRequest(savedUser.getId(), savedWallet.getId(), outflowCategory.getId(), BigDecimal.valueOf(500)));

        assertThat(result.getAmount()).isEqualByComparingTo("500");
        assertThat(result.getCategoryId()).isEqualTo(outflowCategory.getId());
        assertThat(result.getWalletId()).isEqualTo(savedWallet.getId());
    }

    @Test
    void getMonthlySummary_handlesAllOutflowTypesIncludingGenericOutflow() {
        Category fixedCat = new Category();
        fixedCat.setName("Rent");
        fixedCat.setTransactionType(TransactionType.OUTFLOW_FIXED_COST);
        fixedCat.setDefaultCategory(true);
        fixedCat = categoryRepository.save(fixedCat);

        Category genericOutflow = new Category();
        genericOutflow.setName("Misc");
        genericOutflow.setTransactionType(TransactionType.OUTFLOW);
        genericOutflow.setDefaultCategory(true);
        genericOutflow = categoryRepository.save(genericOutflow);

        addExpense(inflowCategory.getId(), BigDecimal.valueOf(100));
        addExpense(fixedCat.getId(), BigDecimal.valueOf(30));
        addExpense(genericOutflow.getId(), BigDecimal.valueOf(20));

        int year = LocalDateTime.now().getYear();
        int month = LocalDateTime.now().getMonthValue();
        MonthlySummaryResponse result = expenseService.getMonthlySummary(savedUser.getId(), year, month);

        assertThat(result.getTotalIncome()).isEqualByComparingTo("100");
        assertThat(result.getTotalExpenses()).isEqualByComparingTo("50");
        assertThat(result.getNetAmount()).isEqualByComparingTo("50");
    }

    private AddExpenseRequest buildRequest(UUID userId, UUID walletId, UUID categoryId, BigDecimal amount) {
        AddExpenseRequest r = new AddExpenseRequest();
        r.setUserId(userId);
        r.setWalletId(walletId);
        r.setCategoryId(categoryId);
        r.setAmount(amount);
        r.setPaymentMethod(PaymentMethod.CASH);
        return r;
    }

    private void addExpense(UUID categoryId, BigDecimal amount) {
        expenseService.addExpense(buildRequest(savedUser.getId(), savedWallet.getId(), categoryId, amount));
    }
}