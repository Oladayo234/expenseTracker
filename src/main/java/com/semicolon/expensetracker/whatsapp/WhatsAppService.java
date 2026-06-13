package com.semicolon.expensetracker.whatsapp;

import com.semicolon.expensetracker.data.models.Expense;
import com.semicolon.expensetracker.data.models.User;
import com.semicolon.expensetracker.data.models.Wallet;
import com.semicolon.expensetracker.data.models.enums.PaymentMethod;
import com.semicolon.expensetracker.data.repositories.CategoryRepository;
import com.semicolon.expensetracker.data.repositories.ExpenseRepository;
import com.semicolon.expensetracker.data.repositories.UserRepository;
import com.semicolon.expensetracker.data.repositories.WalletRepository;
import com.semicolon.expensetracker.dtos.request.AddExpenseRequest;
import com.semicolon.expensetracker.dtos.response.BudgetLimitVsActualExpenseResponse;
import com.semicolon.expensetracker.dtos.response.MonthlySummaryResponse;
import com.semicolon.expensetracker.dtos.response.WalletBalanceResponse;
import com.semicolon.expensetracker.services.BudgetService;
import com.semicolon.expensetracker.services.ExpenseService;
import com.semicolon.expensetracker.services.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WhatsAppService {

    private final MessageParser messageParser;
    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final CategoryRepository categoryRepository;
    private final ExpenseRepository expenseRepository;
    private final ExpenseService expenseService;
    private final WalletService walletService;
    private final BudgetService budgetService;

    public String handle(String from, String body) {
        // Resolve user from phone number
        User user = userRepository.findByPhoneNumber(from).orElse(null);

        if (user == null) {
            return "👋 Your number isn't linked to an account.\n" +
                    "Please register on the app and add this WhatsApp number to your profile.";
        }

        // Set security context so existing services work without modification
        authenticateUser(user);

        ParsedIntent intent = messageParser.parse(body);

        return switch (intent.getIntent()) {
            case ADD_EXPENSE  -> handleAddExpense(intent, user);
            case GET_SUMMARY  -> handleSummary();
            case GET_BUDGETS  -> handleBudgets();
            case GET_WALLETS  -> handleWallets(user);
            case GET_EXPENSES -> handleRecentExpenses(user);
            case HELP, UNKNOWN -> helpText();
        };
    }

    // ── Intent Handlers ──────────────────────────────────────────────────────

    private String handleAddExpense(ParsedIntent intent, User user) {
        var wallet = walletRepository
                .findByUserIdAndNameIgnoreCase(user.getId(), intent.getWallet())
                .orElse(null);

        if (wallet == null) {
            return "❌ Wallet \"" + intent.getWallet() + "\" not found.\n" +
                    "Type *wallets* to see your wallets.";
        }

        var category = categoryRepository
                .findByNameIgnoreCaseAndUserIdOrDefault(intent.getCategory(), user.getId())
                .orElse(null);

        if (category == null) {
            return "❌ Category \"" + intent.getCategory() + "\" not found.\n" +
                    "Check your categories in the app.";
        }

        AddExpenseRequest request = new AddExpenseRequest();
        request.setWalletId(wallet.getPublicId());
        request.setCategoryId(category.getPublicId());
        request.setAmount(intent.getAmount());
        request.setNote(intent.getNote());
        request.setPaymentMethod(PaymentMethod.CASH);

        expenseService.addExpense(request);

        return String.format(
                "✅ Logged ₦%,.2f under *%s* from *%s* wallet.",
                intent.getAmount(),
                capitalize(intent.getCategory()),
                capitalize(intent.getWallet())
        );
    }

    private String handleSummary() {
        LocalDate now = LocalDate.now();
        MonthlySummaryResponse summary =
                expenseService.getMonthlySummary(now.getYear(), now.getMonthValue());
        return String.format(
                "📊 *%s Summary*\n\n" +
                        "Income:    ₦%,.2f\n" +
                        "Expenses:  ₦%,.2f\n" +
                        "Net:       ₦%,.2f",
                now.getMonth().name(),
                summary.getTotalIncome(),
                summary.getTotalExpenses(),
                summary.getNetAmount()
        );
    }

    private String handleBudgets() {
        List<BudgetLimitVsActualExpenseResponse> budgets =
                budgetService.getBudgetVsActual();

        if (budgets.isEmpty()) return "📋 You have no budgets set up yet.";

        StringBuilder sb = new StringBuilder("📋 *Your Budgets*\n\n");
        for (var b : budgets) {
            String emoji = switch (b.getStatus()) {
                case "EXCEEDED" -> "🔴";
                case "WARNING"  -> "🟡";
                default         -> "🟢";
            };
            sb.append(String.format("%s *%s*  ₦%,.2f / ₦%,.2f\n",
                    emoji,
                    b.getCategoryName(),
                    b.getActualSpent(),
                    b.getBudgetAmount()
            ));
        }
        return sb.toString().trim();
    }

    private String handleWallets(User user) {
        List<Wallet> wallets = walletRepository.findByUserId(user.getId());

        if (wallets.isEmpty()) return "👛 You have no wallets yet.";

        StringBuilder sb = new StringBuilder("👛 *Your Wallets*\n\n");
        for (var w : wallets) {
            WalletBalanceResponse balance =
                    walletService.getWalletBalance(w.getPublicId());
            sb.append(String.format("• *%s*  ₦%,.2f\n",
                    w.getName(),
                    balance.getBalance()
            ));
        }
        return sb.toString().trim();
    }

    private String handleRecentExpenses(User user) {
        List<Expense> expenses = expenseRepository
                .findTop5ByWalletUserIdOrderByExpenseDateDesc(user.getId());

        if (expenses.isEmpty()) return "📝 No recent expenses found.";

        StringBuilder sb = new StringBuilder("📝 *Recent Expenses*\n\n");
        for (var e : expenses) {
            sb.append(String.format("• ₦%,.2f  %s  (%s)\n",
                    e.getAmount(),
                    e.getCategory().getName(),
                    e.getExpenseDate().toLocalDate()
            ));
        }
        return sb.toString().trim();
    }

    private String helpText() {
        return """
            🤖 *Expense Tracker Bot*
            
            Here's what I can do:
            
            • *add [amount] [category] [wallet]*
              e.g. add 5000 food GTBank
            
            • *summary* — this month's overview
            • *budgets* — all budgets with status
            • *wallets* — all wallet balances
            • *expenses* — last 5 expenses
            • *help* — show this message
            """;
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void authenticateUser(User user) {
        var auth = new UsernamePasswordAuthenticationToken(
                user, null, List.of()
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
    }
}