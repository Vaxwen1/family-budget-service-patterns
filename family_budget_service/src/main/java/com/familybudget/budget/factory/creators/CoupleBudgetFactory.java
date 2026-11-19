package com.familybudget.budget.factory.creators;

import org.springframework.stereotype.Component;

import com.familybudget.budget.factory.BudgetFactory;
import com.familybudget.budget.factory.BudgetTemplate;
import com.familybudget.budget.factory.CategoryTemplate;
import com.familybudget.budget.factory.StaticBudgetTemplate;

import java.util.List;

@Component
public class CoupleBudgetFactory extends BudgetFactory {

    @Override
    public String supportedFamilyType() {
        return "COUPLE";
    }

    @Override
    protected BudgetTemplate createTemplate() {
        return new StaticBudgetTemplate("Monthly budget", List.of(
                new CategoryTemplate("Housing", "#2196F3"),
                new CategoryTemplate("Groceries", "#4CAF50"),
                new CategoryTemplate("Transport", "#FF9800"),
                new CategoryTemplate("Leisure", "#9C27B0"),
                new CategoryTemplate("Savings", "#00BCD4")
        ));
    }
}
