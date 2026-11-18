package com.familybudget.budget.event;

import com.familybudget.budget.event.enums.ChangeType;
import com.familybudget.budget.messaging.dto.TransactionCreatedEvent;
import com.familybudget.budget.messaging.dto.TransactionUpdatedEvent;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

public record TransactionChangedEvent(
        ChangeType type,
        Long familyId,
        Long budgetId,
        Long categoryId,
        Long transactionId,
        BigDecimal amount,
        String description,
        LocalDate date,
        BigDecimal budgetSpent,
        BigDecimal budgetLimit,
        Instant occurredAt
) implements DomainEvent {

    public TransactionChangedEvent {
        Objects.requireNonNull(type, "type is required");
    }

    @Override
    public String routingKey() {
        return switch (type) {
            case CREATED -> "transaction.created";
            case UPDATED -> "transaction.updated";
        };
    }

    @Override
    public Object toMessage() {
        return switch (type) {
            case CREATED -> new TransactionCreatedEvent(familyId, budgetId, categoryId, transactionId,
                    amount, description, date, occurredAt);
            case UPDATED -> new TransactionUpdatedEvent(familyId, budgetId, categoryId, transactionId,
                    amount, description, date, occurredAt);
        };
    }

    public boolean isOverBudget() {
        return budgetLimit != null && budgetSpent != null
                && budgetLimit.signum() > 0
                && budgetSpent.compareTo(budgetLimit) > 0;
    }
}
