package com.familybudget.budget.dto.data;

import com.familybudget.budget.event.enums.ChangeType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public class BudgetData {
    Long familyId;
    Long budgetId;
    Long categoryId;
    Long transactionId;
    BigDecimal amount;
    String description;
    LocalDate date;
    BigDecimal budgetSpent;
    BigDecimal budgetLimit;
    Instant occurredAt;

}
