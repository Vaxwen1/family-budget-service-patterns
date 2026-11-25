package com.familybudget.budget.services.impl;

import com.familybudget.budget.dto.request.TransactionRequest;
import com.familybudget.budget.entity.Budget;
import com.familybudget.budget.entity.Category;
import com.familybudget.budget.entity.Family;
import com.familybudget.budget.entity.Transaction;
import com.familybudget.budget.event.TransactionChangedEvent;
import com.familybudget.budget.event.enums.ChangeType;
import com.familybudget.budget.exception.BudgetNotFoundException;
import com.familybudget.budget.exception.CategoryNotFoundException;
import com.familybudget.budget.exception.FamilyNotFoundException;
import com.familybudget.budget.exception.TransactionNotFoundException;
import com.familybudget.budget.repository.BudgetRepository;
import com.familybudget.budget.repository.CategoryRepository;
import com.familybudget.budget.repository.FamilyRepository;
import com.familybudget.budget.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceImplTest {

    @Mock private BudgetRepository budgetRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private FamilyRepository familyRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks private TransactionServiceImpl service;

    private Family family;
    private Budget budget;
    private Category category;

    @BeforeEach
    void setUp() {
        family = new Family();
        family.setId(1L);
        family.setType("SINGLE");

        budget = new Budget();
        budget.setId(2L);
        budget.setName("Monthly budget");
        budget.setLimitAmount(new BigDecimal("500.00"));
        budget.setSpent(new BigDecimal("100.00"));
        budget.setFamily(family);

        category = new Category();
        category.setId(3L);
        category.setName("Groceries");
        category.setSpent(new BigDecimal("40.00"));
        category.setBudget(budget);
        budget.getCategories().add(category);
    }

    @Test
    void addTransactionRejectsNullAmount() {
        TransactionRequest request = request(null, "Bread", LocalDate.of(2026, 11, 1));

        assertThatThrownBy(() -> service.addTransaction(1L, 2L, 3L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Transaction amount is required");

        verifyNoInteractions(familyRepository, budgetRepository, categoryRepository,
                transactionRepository, eventPublisher);
    }

    @Test
    void addTransactionFailsWhenFamilyDoesNotExist() {
        when(familyRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.addTransaction(1L, 2L, 3L, validRequest()))
                .isInstanceOf(FamilyNotFoundException.class);

        verifyNoInteractions(transactionRepository, eventPublisher);
    }

    @Test
    void addTransactionFailsWhenBudgetBelongsToAnotherFamily() {
        when(familyRepository.findById(1L)).thenReturn(Optional.of(family));
        when(budgetRepository.findByIdAndFamilyId(2L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.addTransaction(1L, 2L, 3L, validRequest()))
                .isInstanceOf(BudgetNotFoundException.class);

        verifyNoInteractions(transactionRepository, eventPublisher);
    }

    @Test
    void addTransactionFailsWhenCategoryIsNotInTheBudget() {
        when(familyRepository.findById(1L)).thenReturn(Optional.of(family));
        when(budgetRepository.findByIdAndFamilyId(2L, 1L)).thenReturn(Optional.of(budget));
        when(categoryRepository.findByIdAndBudgetIdAndBudgetFamilyId(3L, 2L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.addTransaction(1L, 2L, 3L, validRequest()))
                .isInstanceOf(CategoryNotFoundException.class);

        verifyNoInteractions(transactionRepository, eventPublisher);
    }

    @Test
    void addTransactionSavesTheTransactionAndAddsToBothTotals() {
        stubLookups();
        stubSaveAssigningId();

        Transaction saved = service.addTransaction(1L, 2L, 3L,
                request(new BigDecimal("25.50"), "Bread", LocalDate.of(2026, 11, 1)));

        assertThat(saved.getId()).isEqualTo(50L);
        assertThat(saved.getAmount()).isEqualByComparingTo("25.50");
        assertThat(saved.getDescription()).isEqualTo("Bread");
        assertThat(saved.getDate()).isEqualTo(LocalDate.of(2026, 11, 1));
        assertThat(saved.getCategory()).isSameAs(category);
        assertThat(category.getSpent()).isEqualByComparingTo("65.50");
        assertThat(budget.getSpent()).isEqualByComparingTo("125.50");
    }

    @Test
    void addTransactionPublishesACreatedEventWithTheNewTotals() {
        stubLookups();
        stubSaveAssigningId();

        service.addTransaction(1L, 2L, 3L,
                request(new BigDecimal("25.50"), "Bread", LocalDate.of(2026, 11, 1)));

        TransactionChangedEvent event = capturePublishedEvent();
        assertThat(event.type()).isEqualTo(ChangeType.CREATED);
        assertThat(event.familyId()).isEqualTo(1L);
        assertThat(event.budgetId()).isEqualTo(2L);
        assertThat(event.categoryId()).isEqualTo(3L);
        assertThat(event.transactionId()).isEqualTo(50L);
        assertThat(event.amount()).isEqualByComparingTo("25.50");
        assertThat(event.budgetSpent()).isEqualByComparingTo("125.50");
        assertThat(event.budgetLimit()).isEqualByComparingTo("500.00");
        assertThat(event.isOverBudget()).isFalse();
    }

    @Test
    void addTransactionEventIsOverBudgetWhenTheLimitIsExceeded() {
        budget.setSpent(new BigDecimal("480.00"));
        stubLookups();
        stubSaveAssigningId();

        service.addTransaction(1L, 2L, 3L,
                request(new BigDecimal("30.00"), "Shoes", LocalDate.of(2026, 11, 2)));

        assertThat(capturePublishedEvent().isOverBudget()).isTrue();
    }

    @Test
    void getTransactionsReturnsACopyOfTheCategoryTransactions() {
        when(budgetRepository.findByIdAndFamilyId(2L, 1L)).thenReturn(Optional.of(budget));
        when(categoryRepository.findByIdAndBudgetIdAndBudgetFamilyId(3L, 2L, 1L)).thenReturn(Optional.of(category));
        Transaction first = Transaction.builder().id(10L).amount(BigDecimal.ONE).category(category).build();
        category.setTransactions(new java.util.ArrayList<>(List.of(first)));

        List<Transaction> result = service.getTransactions(1L, 2L, 3L);

        assertThat(result).containsExactly(first);
        assertThat(result).isNotSameAs(category.getTransactions());
    }

    @Test
    void getTransactionsFailsWhenTheBudgetIsNotFound() {
        when(budgetRepository.findByIdAndFamilyId(2L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getTransactions(1L, 2L, 3L))
                .isInstanceOf(BudgetNotFoundException.class);
    }

    @Test
    void getTransactionReturnsTheMatchingTransaction() {
        Transaction existing = Transaction.builder().id(50L).amount(BigDecimal.TEN).category(category).build();
        when(transactionRepository.findByIdAndCategoryIdAndCategoryBudgetIdAndCategoryBudgetFamilyId(50L, 3L, 2L, 1L))
                .thenReturn(Optional.of(existing));

        assertThat(service.getTransaction(1L, 2L, 3L, 50L)).isSameAs(existing);
    }

    @Test
    void getTransactionFailsWhenNoMatchExists() {
        when(transactionRepository.findByIdAndCategoryIdAndCategoryBudgetIdAndCategoryBudgetFamilyId(50L, 3L, 2L, 1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getTransaction(1L, 2L, 3L, 50L))
                .isInstanceOf(TransactionNotFoundException.class);
    }

    @Test
    void updateTransactionAppliesOnlyTheDifferenceToTheTotals() {
        Transaction existing = existingTransaction(new BigDecimal("40.00"));
        stubTransactionLookup(existing);
        stubSaveReturningArgument();

        Transaction saved = service.updateTransaction(1L, 2L, 3L, 50L,
                request(new BigDecimal("60.00"), null, null));

        assertThat(saved.getAmount()).isEqualByComparingTo("60.00");
        assertThat(category.getSpent()).isEqualByComparingTo("60.00");
        assertThat(budget.getSpent()).isEqualByComparingTo("120.00");
        assertThat(capturePublishedEvent().type()).isEqualTo(ChangeType.UPDATED);
    }

    @Test
    void updateTransactionTreatsAMissingOldAmountAsZero() {
        Transaction existing = existingTransaction(null);
        stubTransactionLookup(existing);
        stubSaveReturningArgument();

        service.updateTransaction(1L, 2L, 3L, 50L, request(new BigDecimal("15.00"), null, null));

        assertThat(category.getSpent()).isEqualByComparingTo("55.00");
        assertThat(budget.getSpent()).isEqualByComparingTo("115.00");
    }

    @Test
    void updateTransactionWithEmptyRequestChangesNothingButStillPublishes() {
        Transaction existing = existingTransaction(new BigDecimal("40.00"));
        existing.setDescription("Original");
        stubTransactionLookup(existing);
        stubSaveReturningArgument();

        Transaction saved = service.updateTransaction(1L, 2L, 3L, 50L, request(null, null, null));

        assertThat(saved.getAmount()).isEqualByComparingTo("40.00");
        assertThat(saved.getDescription()).isEqualTo("Original");
        assertThat(category.getSpent()).isEqualByComparingTo("40.00");
        assertThat(budget.getSpent()).isEqualByComparingTo("100.00");
        assertThat(capturePublishedEvent().type()).isEqualTo(ChangeType.UPDATED);
    }

    @Test
    void updateTransactionChangesDescriptionAndDateWithoutTouchingTotals() {
        Transaction existing = existingTransaction(new BigDecimal("40.00"));
        stubTransactionLookup(existing);
        stubSaveReturningArgument();

        Transaction saved = service.updateTransaction(1L, 2L, 3L, 50L,
                request(null, "Corrected", LocalDate.of(2026, 11, 5)));

        assertThat(saved.getDescription()).isEqualTo("Corrected");
        assertThat(saved.getDate()).isEqualTo(LocalDate.of(2026, 11, 5));
        assertThat(category.getSpent()).isEqualByComparingTo("40.00");
        assertThat(budget.getSpent()).isEqualByComparingTo("100.00");
    }

    @Test
    void updateTransactionFailsWhenNoMatchExists() {
        when(transactionRepository.findByIdAndCategoryIdAndCategoryBudgetIdAndCategoryBudgetFamilyId(50L, 3L, 2L, 1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateTransaction(1L, 2L, 3L, 50L, request(BigDecimal.ONE, null, null)))
                .isInstanceOf(TransactionNotFoundException.class);

        verifyNoInteractions(eventPublisher);
    }

    private Transaction existingTransaction(BigDecimal amount) {
        return Transaction.builder().id(50L).amount(amount).category(category).build();
    }

    private void stubLookups() {
        when(familyRepository.findById(1L)).thenReturn(Optional.of(family));
        when(budgetRepository.findByIdAndFamilyId(2L, 1L)).thenReturn(Optional.of(budget));
        when(categoryRepository.findByIdAndBudgetIdAndBudgetFamilyId(3L, 2L, 1L)).thenReturn(Optional.of(category));
    }

    private void stubTransactionLookup(Transaction existing) {
        when(transactionRepository.findByIdAndCategoryIdAndCategoryBudgetIdAndCategoryBudgetFamilyId(50L, 3L, 2L, 1L))
                .thenReturn(Optional.of(existing));
    }

    private void stubSaveAssigningId() {
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction transaction = invocation.getArgument(0);
            if (transaction.getId() == null) {
                transaction.setId(50L);
            }
            return transaction;
        });
    }

    private void stubSaveReturningArgument() {
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private TransactionChangedEvent capturePublishedEvent() {
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(captor.capture());
        return (TransactionChangedEvent) captor.getValue();
    }

    private static TransactionRequest request(BigDecimal amount, String description, LocalDate date) {
        TransactionRequest request = new TransactionRequest();
        request.setAmount(amount);
        request.setDescription(description);
        request.setDate(date);
        return request;
    }

    private static TransactionRequest validRequest() {
        return request(new BigDecimal("10.00"), "Milk", LocalDate.of(2026, 11, 1));
    }
}
