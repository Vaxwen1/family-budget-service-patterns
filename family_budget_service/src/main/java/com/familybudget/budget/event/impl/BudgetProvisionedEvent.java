package com.familybudget.budget.event.impl;

import com.familybudget.budget.event.DomainEvent;
import com.familybudget.budget.messaging.dto.BudgetCreatedEvent;

import java.math.BigDecimal;
import java.time.Instant;

public record BudgetProvisionedEvent(Long familyId, Long budgetId, String name,
                                     BigDecimal limitAmount, Instant occurredAt) implements DomainEvent  {

    @Override
    public String routingKey() {
        return "budget.create";
    }

    @Override
    public Object toMessage() {
        return new BudgetCreatedEvent(familyId, budgetId, name, limitAmount, occurredAt);
    }
}
