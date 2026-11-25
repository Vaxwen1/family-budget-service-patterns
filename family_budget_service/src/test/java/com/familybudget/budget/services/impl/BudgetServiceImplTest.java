package com.familybudget.budget.services.impl;

import com.familybudget.budget.dto.request.BudgetRequest;
import com.familybudget.budget.entity.Budget;
import com.familybudget.budget.entity.Family;
import com.familybudget.budget.exception.BudgetNotFoundException;
import com.familybudget.budget.exception.FamilyNotFoundException;
import com.familybudget.budget.messaging.dto.BudgetCreatedEvent;
import com.familybudget.budget.messaging.dto.BudgetUpdatedEvent;
import com.familybudget.budget.messaging.publisher.DomainEventPublisher;
import com.familybudget.budget.repository.BudgetRepository;
import com.familybudget.budget.repository.FamilyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
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
class BudgetServiceImplTest {

    @Mock private BudgetRepository budgetRepository;
    @Mock private FamilyRepository familyRepository;
    @Mock private DomainEventPublisher publisher;

    @InjectMocks private BudgetServiceImpl service;

    private Family family;

    @BeforeEach
    void setUp() {
        family = new Family();
        family.setId(1L);
        family.setType("SINGLE");
    }

    @Test
    void createBudgetLinksTheFamilySavesAndPublishesCreated() {
        when(familyRepository.findById(1L)).thenReturn(Optional.of(family));
        when(budgetRepository.save(any(Budget.class))).thenAnswer(invocation -> {
            Budget budget = invocation.getArgument(0);
            budget.setId(7L);
            return budget;
        });
        BudgetRequest request = request("Holiday", "1200.00");

        Budget saved = service.createBudget(1L, request);

        assertThat(saved.getId()).isEqualTo(7L);
        assertThat(saved.getName()).isEqualTo("Holiday");
        assertThat(saved.getLimitAmount()).isEqualByComparingTo("1200.00");
        assertThat(saved.getFamily()).isSameAs(family);

        ArgumentCaptor<Object> event = ArgumentCaptor.forClass(Object.class);
        verify(publisher).publish(eq("budget.created"), event.capture());
        assertThat(event.getValue()).isInstanceOf(BudgetCreatedEvent.class);
    }

    @Test
    void createBudgetFailsWhenFamilyDoesNotExist() {
        when(familyRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createBudget(1L, request("Holiday", "1200.00")))
                .isInstanceOf(FamilyNotFoundException.class);

        verifyNoInteractions(budgetRepository, publisher);
    }

    @Test
    void getAllBudgetsReturnsTheFamilyBudgets() {
        List<Budget> budgets = List.of(new Budget());
        when(familyRepository.findById(1L)).thenReturn(Optional.of(family));
        when(budgetRepository.findByFamilyId(1L)).thenReturn(budgets);

        assertThat(service.getAllBudgets(1L)).isSameAs(budgets);
    }

    @Test
    void getAllBudgetsFailsWhenFamilyDoesNotExist() {
        when(familyRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getAllBudgets(1L))
                .isInstanceOf(FamilyNotFoundException.class);

        verifyNoInteractions(budgetRepository);
    }

    @Test
    void getBudgetReturnsTheBudgetInsideTheFamily() {
        Budget budget = new Budget();
        when(familyRepository.findById(1L)).thenReturn(Optional.of(family));
        when(budgetRepository.findByIdAndFamilyId(7L, 1L)).thenReturn(Optional.of(budget));

        assertThat(service.getBudget(1L, 7L)).isSameAs(budget);
    }

    @Test
    void getBudgetFailsWhenTheBudgetIsNotInThatFamily() {
        when(familyRepository.findById(1L)).thenReturn(Optional.of(family));
        when(budgetRepository.findByIdAndFamilyId(7L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getBudget(1L, 7L))
                .isInstanceOf(BudgetNotFoundException.class);
    }

    @Test
    void getBudgetFailsWhenFamilyDoesNotExist() {
        when(familyRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getBudget(1L, 7L))
                .isInstanceOf(FamilyNotFoundException.class);
    }

    @Test
    void updateBudgetChangesOnlyTheFieldsThatAreSent() {
        Budget existing = existingBudget("Holiday", "1200.00");
        when(familyRepository.findById(1L)).thenReturn(Optional.of(family));
        when(budgetRepository.findByIdAndFamilyId(7L, 1L)).thenReturn(Optional.of(existing));
        when(budgetRepository.save(any(Budget.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Budget saved = service.updateBudget(1L, 7L, request(null, "1500.00"));

        assertThat(saved.getName()).isEqualTo("Holiday");
        assertThat(saved.getLimitAmount()).isEqualByComparingTo("1500.00");
        verify(publisher).publish(eq("budget.updated"), any(BudgetUpdatedEvent.class));
    }

    @Test
    void updateBudgetRenamesWhenOnlyTheNameIsSent() {
        Budget existing = existingBudget("Holiday", "1200.00");
        when(familyRepository.findById(1L)).thenReturn(Optional.of(family));
        when(budgetRepository.findByIdAndFamilyId(7L, 1L)).thenReturn(Optional.of(existing));
        when(budgetRepository.save(any(Budget.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Budget saved = service.updateBudget(1L, 7L, request("Summer", null));

        assertThat(saved.getName()).isEqualTo("Summer");
        assertThat(saved.getLimitAmount()).isEqualByComparingTo("1200.00");
    }

    @Test
    void updateBudgetFailsWhenTheBudgetIsNotFound() {
        when(familyRepository.findById(1L)).thenReturn(Optional.of(family));
        when(budgetRepository.findByIdAndFamilyId(7L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateBudget(1L, 7L, request("Summer", null)))
                .isInstanceOf(BudgetNotFoundException.class);

        verifyNoInteractions(publisher);
    }

    @Test
    void updateBudgetFailsWhenFamilyDoesNotExist() {
        when(familyRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateBudget(1L, 7L, request("Summer", null)))
                .isInstanceOf(FamilyNotFoundException.class);

        verifyNoInteractions(budgetRepository, publisher);
    }

    private static Budget existingBudget(String name, String limit) {
        Budget budget = new Budget();
        budget.setId(7L);
        budget.setName(name);
        budget.setLimitAmount(new BigDecimal(limit));
        budget.setSpent(BigDecimal.ZERO);
        return budget;
    }

    private static BudgetRequest request(String name, String limit) {
        BudgetRequest request = new BudgetRequest();
        request.setName(name);
        request.setLimitAmount(limit == null ? null : new BigDecimal(limit));
        return request;
    }
}
