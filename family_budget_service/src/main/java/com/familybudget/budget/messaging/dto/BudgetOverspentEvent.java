package com.familybudget.budget.messaging.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record BudgetOverspentEvent(
        Long familyId,
        Long budgetId,
        BigDecimal spent,
        BigDecimal limitAmount,
        Instant occurredAt
) {}
