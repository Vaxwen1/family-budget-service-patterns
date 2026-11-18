package com.familybudget.budget.event;

public interface DomainEvent {
    public String routingKey();
    Object toMessage();
}
