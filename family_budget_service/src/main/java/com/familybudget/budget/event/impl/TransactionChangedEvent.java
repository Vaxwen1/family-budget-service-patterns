package com.familybudget.budget.event.impl;

import com.familybudget.budget.event.DomainEvent;
import com.familybudget.budget.event.enums.ChageType;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
public class TransactionChangedEvent implements DomainEvent {
    private ChageType type;
    private Long familyId;
    private Long budgetId;
    private Long categoryId;
    private Long transactionId;
    private BigDecimal amount;
    private String description;
    private LocalDate date;
    private BigDecimal budgetSpent;
    private BigDecimal budgetLimit;
    private Instant occurredAt;

    @Override
    public String routingKey() {
        return switch (type) {
            case CREATED -> "transaction.created";
            case UPDATED -> "transaction.updated";
            case DELETED -> "transaction.deleted";
        };
    }

    @Override
    public Object toMessage() {
        return null;
    }

    public boolean isOverBudget() {
        return budgetLimit != null && budgetSpent != null
                && budgetLimit.signum() > 0
                && budgetSpent.compareTo(budgetLimit) > 0;
    }
}
