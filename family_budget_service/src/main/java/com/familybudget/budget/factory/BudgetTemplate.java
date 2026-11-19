package com.familybudget.budget.factory;

import java.util.List;

public interface BudgetTemplate {
    String defaultName();
    List<CategoryTemplate> categories();
}
