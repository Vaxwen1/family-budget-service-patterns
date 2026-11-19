package com.familybudget.budget.factory;

import com.familybudget.budget.entity.Budget;
import com.familybudget.budget.entity.Category;
import com.familybudget.budget.entity.Family;
import java.math.BigDecimal;

public abstract class BudgetFactory {

    public abstract String supportedFamilyType();

    protected abstract BudgetTemplate createTemplate();
    
    public Budget createBudget(Family family, BigDecimal limitAmount) {
        BudgetTemplate template = createTemplate();
        Budget budget = new Budget();
        budget.setName(template.defaultName());
        budget.setFamily(family);
        budget.setSpent(BigDecimal.ZERO);
        budget.setLimitAmount(limitAmount);

        for (CategoryTemplate categoryTemplate : template.categories()) {
            Category category = new Category();
            category.setName(categoryTemplate.name());
            category.setBudget(budget);
            category.setColor(categoryTemplate.color());
            category.setSpent(BigDecimal.ZERO);
            budget.getCategories().add(category);
        }
        return budget;
    }
    
}
