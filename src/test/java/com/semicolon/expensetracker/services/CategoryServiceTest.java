package com.semicolon.expensetracker.services;

import com.semicolon.expensetracker.data.models.User;
import com.semicolon.expensetracker.data.models.enums.TransactionType;
import com.semicolon.expensetracker.data.repositories.UserRepository;
import com.semicolon.expensetracker.dtos.request.CreateCategoryRequest;
import com.semicolon.expensetracker.dtos.request.DeleteCategoryRequest;
import com.semicolon.expensetracker.dtos.response.CreateCategoryResponse;
import com.semicolon.expensetracker.exceptions.InvalidEntryException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
class CategoryServiceTest {

    @Autowired CategoryService categoryService;
    @Autowired UserRepository userRepository;

    private User savedUser;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setEmail("cat@test.com");
        user.setUsername("catuser");
        user.setName("Cat User");
        user.setPassword("hash");
        user.setPhoneNumber("07000000000");
        savedUser = userRepository.save(user);
    }

    @Test
    void createCategory_duplicateNameForSameUser_throwsInvalidEntryException() {
        categoryService.createCategory(buildRequest("Transport", savedUser.getId()));
        assertThrows(InvalidEntryException.class,
                () -> categoryService.createCategory(buildRequest("Transport", savedUser.getId())));
    }

    @Test
    void createCategory_success_returnsResponseWithCorrectFields() {
        CreateCategoryResponse result = categoryService.createCategory(buildRequest("Housing", savedUser.getId()));
        assertThat(result.getName()).isEqualTo("Housing");
        assertThat(result.isDefault()).isFalse();
        assertThat(result.getId()).isNotNull();
    }

    @Test
    void deleteCategory_defaultCategory_throwsInvalidEntryException() {
        CreateCategoryRequest defaultReq = new CreateCategoryRequest();
        defaultReq.setName("DefaultCat");
        defaultReq.setTransactionType(TransactionType.INFLOW);
        defaultReq.setUserId(null);
        CreateCategoryResponse created = categoryService.createCategory(defaultReq);

        DeleteCategoryRequest deleteReq = new DeleteCategoryRequest();
        deleteReq.setCategoryId(created.getId());
        deleteReq.setUserId(savedUser.getId());

        assertThrows(InvalidEntryException.class, () -> categoryService.deleteCategory(deleteReq));
    }

    @Test
    void deleteCategory_notOwnedByUser_throwsInvalidEntryException() {
        User other = new User();
        other.setEmail("other@cat.com");
        other.setUsername("othercat");
        other.setName("Other");
        other.setPassword("hash");
        other.setPhoneNumber("06000000000");
        User savedOther = userRepository.save(other);

        CreateCategoryResponse created = categoryService.createCategory(buildRequest("Personal", savedOther.getId()));

        DeleteCategoryRequest deleteReq = new DeleteCategoryRequest();
        deleteReq.setCategoryId(created.getId());
        deleteReq.setUserId(savedUser.getId());

        assertThrows(InvalidEntryException.class, () -> categoryService.deleteCategory(deleteReq));
    }

    @Test
    void deleteCategory_reassignsExpensesAndDeletesCategory() {
        CreateCategoryResponse created = categoryService.createCategory(buildRequest("Shopping", savedUser.getId()));

        DeleteCategoryRequest deleteReq = new DeleteCategoryRequest();
        deleteReq.setCategoryId(created.getId());
        deleteReq.setUserId(savedUser.getId());

        categoryService.deleteCategory(deleteReq);

        assertThat(categoryService.getCategories(savedUser.getId()))
                .noneMatch(c -> c.getId().equals(created.getId()));
    }

    private CreateCategoryRequest buildRequest(String name, java.util.UUID userId) {
        CreateCategoryRequest r = new CreateCategoryRequest();
        r.setName(name);
        r.setTransactionType(TransactionType.OUTFLOW_VARIABLE_COST);
        r.setUserId(userId);
        return r;
    }
}