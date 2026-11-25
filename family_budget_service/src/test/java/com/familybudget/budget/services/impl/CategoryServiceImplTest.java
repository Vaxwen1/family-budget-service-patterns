package com.familybudget.budget.services.impl;

import com.familybudget.budget.dto.request.CategoryRequest;
import com.familybudget.budget.entity.Budget;
import com.familybudget.budget.entity.Category;
import com.familybudget.budget.entity.Family;
import com.familybudget.budget.exception.BudgetNotFoundException;
import com.familybudget.budget.exception.CategoryNotFoundException;
import com.familybudget.budget.exception.FamilyNotFoundException;
import com.familybudget.budget.messaging.dto.CategoryCreatedEvent;
import com.familybudget.budget.messaging.dto.CategoryUpdatedEvent;
import com.familybudget.budget.messaging.publisher.DomainEventPublisher;
import com.familybudget.budget.repository.BudgetRepository;
import com.familybudget.budget.repository.CategoryRepository;
import com.familybudget.budget.repository.FamilyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Mock private BudgetRepository budgetRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private FamilyRepository familyRepository;
    @Mock private DomainEventPublisher publisher;

    @InjectMocks private CategoryServiceImpl service;

    private Family family;
    private Budget budget;

    @BeforeEach
    void setUp() {
        family = new Family();
        family.setId(1L);
        family.setType("SINGLE");

        budget = new Budget();
        budget.setId(2L);
        budget.setFamily(family);
    }

    @Test
    void addCategoryLinksTheBudgetSavesAndPublishesCreated() {
        when(familyRepository.findById(1L)).thenReturn(Optional.of(family));
        when(budgetRepository.findByIdAndFamilyId(2L, 1L)).thenReturn(Optional.of(budget));
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> {
            Category category = invocation.getArgument(0);
            category.setId(3L);
            return category;
        });

        Category saved = service.addCategory(1L, 2L, request("Groceries", "#4CAF50"));

        assertThat(saved.getId()).isEqualTo(3L);
        assertThat(saved.getName()).isEqualTo("Groceries");
        assertThat(saved.getColor()).isEqualTo("#4CAF50");
        assertThat(saved.getBudget()).isSameAs(budget);
        verify(publisher).publish(eq("category.created"), any(CategoryCreatedEvent.class));
    }

    @Test
    void addCategoryFailsWhenFamilyDoesNotExist() {
        when(familyRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.addCategory(1L, 2L, request("Groceries", "#4CAF50")))
                .isInstanceOf(FamilyNotFoundException.class);

        verifyNoInteractions(categoryRepository, publisher);
    }

    @Test
    void addCategoryFailsWhenBudgetIsNotInTheFamily() {
        when(familyRepository.findById(1L)).thenReturn(Optional.of(family));
        when(budgetRepository.findByIdAndFamilyId(2L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.addCategory(1L, 2L, request("Groceries", "#4CAF50")))
                .isInstanceOf(BudgetNotFoundException.class);

        verifyNoInteractions(categoryRepository, publisher);
    }

    @Test
    void getCategoriesReturnsTheBudgetCategories() {
        List<Category> categories = List.of(new Category());
        when(familyRepository.findById(1L)).thenReturn(Optional.of(family));
        when(categoryRepository.findByBudgetIdAndBudgetFamilyId(2L, 1L)).thenReturn(categories);

        assertThat(service.getCategories(1L, 2L)).isSameAs(categories);
    }

    @Test
    void getCategoriesFailsWhenFamilyDoesNotExist() {
        when(familyRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getCategories(1L, 2L))
                .isInstanceOf(FamilyNotFoundException.class);
    }

    @Test
    void getCategoryReturnsTheCategory() {
        Category category = new Category();
        when(familyRepository.findById(1L)).thenReturn(Optional.of(family));
        when(categoryRepository.findByIdAndBudgetIdAndBudgetFamilyId(3L, 2L, 1L)).thenReturn(Optional.of(category));

        assertThat(service.getCategory(1L, 2L, 3L)).isSameAs(category);
    }

    @Test
    void getCategoryFailsWhenMissing() {
        when(familyRepository.findById(1L)).thenReturn(Optional.of(family));
        when(categoryRepository.findByIdAndBudgetIdAndBudgetFamilyId(3L, 2L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getCategory(1L, 2L, 3L))
                .isInstanceOf(CategoryNotFoundException.class);
    }

    @Test
    void getCategoryFailsWhenFamilyDoesNotExist() {
        when(familyRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getCategory(1L, 2L, 3L))
                .isInstanceOf(FamilyNotFoundException.class);
    }

    @Test
    void updateCategoryChangesNameAndColorAndPublishesUpdated() {
        Category existing = category("Groceries", "#4CAF50");
        when(familyRepository.findById(1L)).thenReturn(Optional.of(family));
        when(categoryRepository.findByIdAndBudgetIdAndBudgetFamilyId(3L, 2L, 1L)).thenReturn(Optional.of(existing));
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Category saved = service.updateCategory(1L, 2L, 3L, request("Food", "#00FF00"));

        assertThat(saved.getName()).isEqualTo("Food");
        assertThat(saved.getColor()).isEqualTo("#00FF00");
        verify(publisher).publish(eq("category.updated"), any(CategoryUpdatedEvent.class));
    }

    @Test
    void updateCategoryKeepsFieldsThatAreNotSent() {
        Category existing = category("Groceries", "#4CAF50");
        when(familyRepository.findById(1L)).thenReturn(Optional.of(family));
        when(categoryRepository.findByIdAndBudgetIdAndBudgetFamilyId(3L, 2L, 1L)).thenReturn(Optional.of(existing));
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Category saved = service.updateCategory(1L, 2L, 3L, new CategoryRequest());

        assertThat(saved.getName()).isEqualTo("Groceries");
        assertThat(saved.getColor()).isEqualTo("#4CAF50");
    }

    @Test
    void updateCategoryFailsWhenMissingAndPublishesNothing() {
        when(familyRepository.findById(1L)).thenReturn(Optional.of(family));
        when(categoryRepository.findByIdAndBudgetIdAndBudgetFamilyId(3L, 2L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateCategory(1L, 2L, 3L, request("Food", null)))
                .isInstanceOf(CategoryNotFoundException.class);

        verifyNoInteractions(publisher);
    }

    @Test
    void updateCategoryFailsWhenFamilyDoesNotExist() {
        when(familyRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateCategory(1L, 2L, 3L, request("Food", null)))
                .isInstanceOf(FamilyNotFoundException.class);

        verifyNoInteractions(categoryRepository, publisher);
    }

    private static Category category(String name, String color) {
        Category category = new Category();
        category.setId(3L);
        category.setName(name);
        category.setColor(color);
        return category;
    }

    private static CategoryRequest request(String name, String color) {
        CategoryRequest request = new CategoryRequest();
        request.setName(name);
        request.setColor(color);
        return request;
    }
}
