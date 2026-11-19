package com.familybudget.budget.services.impl;

import com.familybudget.budget.entity.Budget;
import com.familybudget.budget.entity.Family;
import com.familybudget.budget.event.BudgetProvisionedEvent;
import com.familybudget.budget.exception.FamilyNotFoundException;
import com.familybudget.budget.factory.BudgetFactoryRegistry;
import com.familybudget.budget.repository.BudgetRepository;
import com.familybudget.budget.repository.FamilyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.familybudget.budget.services.BudgetProvisioningService;

import java.math.BigDecimal;
import java.time.Instant;


@Service
@RequiredArgsConstructor
public class BudgetProvisionServiceImpl implements BudgetProvisioningService {
    private final FamilyRepository familyRepository;
    private final BudgetRepository budgetRepository;
    private final BudgetFactoryRegistry factoryRegistry;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Budget provisionBudget(Long familyId, BigDecimal limitAmount) {
        Family family = familyRepository.findById(familyId)
                .orElseThrow(() -> new FamilyNotFoundException(familyId));

        Budget budget = factoryRegistry.forFamilyType(family.getType()).createBudget(family, limitAmount);
        Budget saved = budgetRepository.save(budget);

        eventPublisher.publishEvent(new BudgetProvisionedEvent(
                familyId, saved.getId(), saved.getName(), saved.getLimitAmount(), Instant.now()));
        return saved;
    }
    
}
