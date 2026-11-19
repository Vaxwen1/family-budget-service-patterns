package com.familybudget.budget.factory;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SingleBudgetFactory extends BudgetFactory {

    @Override
    public String supportedFamilyType() {
        return "SINGLE";
    }

    @Override
    protected BudgetTemplate createTemplate() {
        return new StaticBudgetTemplate("Monthly budget", List.of(
                new CategoryTemplate("Housing", "#2196F3"),
                new CategoryTemplate("Groceries", "#4CAF50"),
                new CategoryTemplate("Transport", "#FF9800"),
                new CategoryTemplate("Leisure", "#9C27B0")
        ));
    }
}
