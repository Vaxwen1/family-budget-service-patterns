package com.familybudget.budget.factory;
import java.util.List;

public record StaticBudgetTemplate(String defaultName, List<CategoryTemplate> categories) implements BudgetTemplate {
    public StaticBudgetTemplate {
        categories = List.copyOf(categories);
    }
}
