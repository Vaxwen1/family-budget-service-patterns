package com.familybudget.budget.services.impl;

import com.familybudget.budget.entity.Budget;
import com.familybudget.budget.entity.Family;
import com.familybudget.budget.event.BudgetProvisionedEvent;
import com.familybudget.budget.exception.FamilyNotFoundException;
import com.familybudget.budget.factory.BudgetFactoryRegistry;
import com.familybudget.budget.factory.creators.CoupleBudgetFactory;
import com.familybudget.budget.factory.creators.FamilyWithChildrenBudgetFactory;
import com.familybudget.budget.factory.creators.SingleBudgetFactory;
import com.familybudget.budget.factory.creators.StandardBudgetFactory;
import com.familybudget.budget.repository.BudgetRepository;
import com.familybudget.budget.repository.FamilyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BudgetProvisionServiceImplTest {

    @Mock private FamilyRepository familyRepository;
    @Mock private BudgetRepository budgetRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    private BudgetProvisionServiceImpl service;

    @BeforeEach
    void setUp() {
        BudgetFactoryRegistry registry = new BudgetFactoryRegistry(List.of(
                new SingleBudgetFactory(),
                new CoupleBudgetFactory(),
                new FamilyWithChildrenBudgetFactory(),
                new StandardBudgetFactory()));
        service = new BudgetProvisionServiceImpl(familyRepository, budgetRepository, registry, eventPublisher);
    }

    @Test
    void provisionBudgetFailsWhenFamilyDoesNotExist() {
        when(familyRepository.findById(4L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.provisionBudget(4L, new BigDecimal("1000.00")))
                .isInstanceOf(FamilyNotFoundException.class);

        verifyNoInteractions(budgetRepository, eventPublisher);
    }

    @Test
    void provisionBudgetBuildsTheCoupleTemplateAndSavesIt() {
        stubFamily(4L, "COUPLE");
        when(budgetRepository.save(any(Budget.class))).thenAnswer(invocation -> {
            Budget budget = invocation.getArgument(0);
            budget.setId(9L);
            return budget;
        });

        Budget saved = service.provisionBudget(4L, new BigDecimal("1000.00"));

        assertThat(saved.getId()).isEqualTo(9L);
        assertThat(saved.getName()).isEqualTo("Monthly budget");
        assertThat(saved.getLimitAmount()).isEqualByComparingTo("1000.00");
        assertThat(saved.getSpent()).isEqualByComparingTo("0");
        assertThat(saved.getFamily().getId()).isEqualTo(4L);
        assertThat(saved.getCategories()).hasSize(5);
    }

    @Test
    void provisionBudgetUsesTheFamilyWithChildrenTemplate() {
        stubFamily(5L, "FAMILY_WITH_CHILDREN");
        stubSaveReturningArgument();

        Budget saved = service.provisionBudget(5L, new BigDecimal("2000.00"));

        assertThat(saved.getCategories()).hasSize(7);
    }

    @Test
    void provisionBudgetFallsBackToStandardForAnUnknownFamilyType() {
        stubFamily(6L, "UNKNOWN");
        stubSaveReturningArgument();

        Budget saved = service.provisionBudget(6L, new BigDecimal("300.00"));

        assertThat(saved.getCategories()).hasSize(4);
        assertThat(saved.getCategories()).anySatisfy(category ->
                assertThat(category.getName()).isEqualTo("Other"));
    }

    @Test
    void provisionBudgetPublishesABudgetProvisionedEvent() {
        stubFamily(4L, "SINGLE");
        when(budgetRepository.save(any(Budget.class))).thenAnswer(invocation -> {
            Budget budget = invocation.getArgument(0);
            budget.setId(9L);
            return budget;
        });

        service.provisionBudget(4L, new BigDecimal("800.00"));

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(captor.capture());
        BudgetProvisionedEvent event = (BudgetProvisionedEvent) captor.getValue();
        assertThat(event.familyId()).isEqualTo(4L);
        assertThat(event.budgetId()).isEqualTo(9L);
        assertThat(event.name()).isEqualTo("Monthly budget");
        assertThat(event.limitAmount()).isEqualByComparingTo("800.00");
        assertThat(event.occurredAt()).isNotNull();
    }

    private void stubFamily(Long id, String type) {
        Family family = new Family();
        family.setId(id);
        family.setType(type);
        when(familyRepository.findById(id)).thenReturn(Optional.of(family));
    }

    private void stubSaveReturningArgument() {
        when(budgetRepository.save(any(Budget.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }
}
