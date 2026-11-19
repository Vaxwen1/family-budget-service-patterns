package com.familybudget.budget.dto.view;

import java.math.BigDecimal;
import java.util.List;

public record DashboardView(Long familyId, String familyType, int memberCount, BigDecimal income,
                            BigDecimal totalBudgeted, BigDecimal totalSpent, BigDecimal left,
                            List<BudgetSummary> budgets) {}
