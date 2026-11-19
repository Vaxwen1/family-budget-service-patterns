package com.familybudget.budget.services;

import java.math.BigDecimal;

import com.familybudget.budget.entity.Budget;

public interface BudgetProvisioningService {

    public Budget provisionBudget(Long familyId, BigDecimal limitAmount);
    
}
