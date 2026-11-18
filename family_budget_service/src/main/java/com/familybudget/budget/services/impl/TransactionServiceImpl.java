package com.familybudget.budget.services.impl;

import com.familybudget.budget.dto.request.TransactionRequest;
import com.familybudget.budget.entity.Budget;
import com.familybudget.budget.entity.Category;
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
import com.familybudget.budget.services.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final BudgetRepository budgetRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final FamilyRepository familyRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public Transaction addTransaction(Long familyId, Long budgetId, Long categoryId, TransactionRequest request) {
        if (request.getAmount() == null) {
            throw new IllegalArgumentException("Transaction amount is required");
        }

        familyRepository.findById(familyId)
                .orElseThrow(() -> new FamilyNotFoundException(familyId));
        Budget budget = findBudget(familyId, budgetId);
        Category category = findCategory(familyId, budgetId, categoryId);

        BigDecimal amount = request.getAmount();
        Transaction saved = transactionRepository.save(Transaction.builder()
                .amount(amount)
                .description(request.getDescription())
                .date(request.getDate())
                .category(category)
                .build());

        category.setSpent(category.getSpent().add(amount));
        budget.setSpent(budget.getSpent().add(amount));

        eventPublisher.publishEvent(changed(ChangeType.CREATED, familyId, budget, categoryId, saved));
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Transaction> getTransactions(Long familyId, Long budgetId, Long categoryId) {
        findBudget(familyId, budgetId);
        Category category = findCategory(familyId, budgetId, categoryId);
        return new ArrayList<>(category.getTransactions());
    }

    @Override
    @Transactional(readOnly = true)
    public Transaction getTransaction(Long familyId, Long budgetId, Long categoryId, Long transactionId) {
        return transactionRepository
                .findByIdAndCategoryIdAndCategoryBudgetIdAndCategoryBudgetFamilyId(transactionId, categoryId, budgetId, familyId)
                .orElseThrow(() -> new TransactionNotFoundException(transactionId));
    }

    @Override
    public Transaction updateTransaction(Long familyId, Long budgetId, Long categoryId, Long transactionId, TransactionRequest request) {
        Transaction transaction = transactionRepository
                .findByIdAndCategoryIdAndCategoryBudgetIdAndCategoryBudgetFamilyId(transactionId, categoryId, budgetId, familyId)
                .orElseThrow(() -> new TransactionNotFoundException(transactionId));

        Category category = transaction.getCategory();
        Budget budget = category.getBudget();

        if (request.getAmount() != null) {
            BigDecimal oldAmount = transaction.getAmount() == null ? BigDecimal.ZERO : transaction.getAmount();
            BigDecimal delta = request.getAmount().subtract(oldAmount);

            category.setSpent(category.getSpent().add(delta));
            budget.setSpent(budget.getSpent().add(delta));
            transaction.setAmount(request.getAmount());
        }
        if (request.getDescription() != null) {
            transaction.setDescription(request.getDescription());
        }
        if (request.getDate() != null) {
            transaction.setDate(request.getDate());
        }

        Transaction saved = transactionRepository.save(transaction);
        eventPublisher.publishEvent(changed(ChangeType.UPDATED, familyId, budget, category.getId(), saved));
        return saved;
    }

    private Budget findBudget(Long familyId, Long budgetId) {
        return budgetRepository.findByIdAndFamilyId(budgetId, familyId)
                .orElseThrow(() -> new BudgetNotFoundException(budgetId));
    }

    private Category findCategory(Long familyId, Long budgetId, Long categoryId) {
        return categoryRepository.findByIdAndBudgetIdAndBudgetFamilyId(categoryId, budgetId, familyId)
                .orElseThrow(() -> new CategoryNotFoundException(categoryId));
    }

    private TransactionChangedEvent changed(ChangeType type, Long familyId, Budget budget, Long categoryId, Transaction t) {
        return new TransactionChangedEvent(type, familyId, budget.getId(), categoryId, t.getId(),
                t.getAmount(), t.getDescription(), t.getDate(),
                budget.getSpent(), budget.getLimitAmount(), Instant.now());
    }
}
