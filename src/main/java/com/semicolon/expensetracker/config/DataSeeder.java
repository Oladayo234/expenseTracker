package com.semicolon.expensetracker.config;

import com.semicolon.expensetracker.data.models.Category;
import com.semicolon.expensetracker.data.models.enums.TransactionType;
import com.semicolon.expensetracker.data.repositories.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements ApplicationRunner {

    private final CategoryRepository categoryRepository;

    @Override
    public void run(ApplicationArguments args) {
        seedDefaultCategories();
    }

    private void seedDefaultCategories() {
        List<DefaultCategory> defaults = List.of(
                new DefaultCategory("Salary",       "💼", TransactionType.INFLOW),
                new DefaultCategory("Freelance",    "💻", TransactionType.INFLOW),
                new DefaultCategory("Investment",   "📈", TransactionType.INFLOW),
                new DefaultCategory("Gift",         "🎁", TransactionType.INFLOW),
                new DefaultCategory("Rent",         "🏠", TransactionType.OUTFLOW_FIXED_COST),
                new DefaultCategory("Utilities",    "💡", TransactionType.OUTFLOW_FIXED_COST),
                new DefaultCategory("Insurance",    "🛡️", TransactionType.OUTFLOW_FIXED_COST),
                new DefaultCategory("Subscription", "📦", TransactionType.OUTFLOW_FIXED_COST),
                new DefaultCategory("Food",         "🍽️", TransactionType.OUTFLOW_VARIABLE_COST),
                new DefaultCategory("Transport",    "🚗", TransactionType.OUTFLOW_VARIABLE_COST),
                new DefaultCategory("Shopping",     "🛍️", TransactionType.OUTFLOW_VARIABLE_COST),
                new DefaultCategory("Health",       "💊", TransactionType.OUTFLOW_VARIABLE_COST),
                new DefaultCategory("Education",    "📚", TransactionType.OUTFLOW_VARIABLE_COST),
                new DefaultCategory("Entertainment","🎬", TransactionType.OUTFLOW_VARIABLE_COST),
                new DefaultCategory("Data/Airtime", "📱", TransactionType.OUTFLOW_VARIABLE_COST),
                new DefaultCategory("Savings",      "🏦", TransactionType.OUTFLOW),
                new DefaultCategory("Other",        "📌", TransactionType.OUTFLOW)
        );

        int seeded = 0;
        for (DefaultCategory d : defaults) {
            boolean exists = categoryRepository
                    .existsByNameAndDefaultCategoryTrue(d.name());
            if (!exists) {
                Category category = new Category();
                category.setName(d.name());
                category.setIcon(d.icon());
                category.setTransactionType(d.type());
                category.setDefaultCategory(true);
                category.setUser(null);
                categoryRepository.save(category);
                seeded++;
            }
        }

        if (seeded > 0) {
            log.info("DataSeeder: seeded {} default categories", seeded);
        } else {
            log.info("DataSeeder: default categories already present, skipping");
        }
    }

    private record DefaultCategory(String name, String icon, TransactionType type) {}
}