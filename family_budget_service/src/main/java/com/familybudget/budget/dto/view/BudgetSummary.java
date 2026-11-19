package com.familybudget.budget.dto.view;

import java.math.BigDecimal;
import java.util.List;

public record BudgetSummary(Long id, String name, BigDecimal limitAmount, BigDecimal spent,
                            BigDecimal remaining, BigDecimal percentUsed, BigDecimal barPercent,
                            boolean overspent, List<CategorySummary> categories) {}
