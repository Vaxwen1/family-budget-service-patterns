package com.familybudget.budget.observer;

import com.familybudget.budget.event.impl.TransactionChangedEvent;
import com.familybudget.budget.messaging.dto.BudgetOverspentEvent;
import com.familybudget.budget.messaging.publisher.DomainEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class OverspendAlertObserver {
    private final DomainEventPublisher publisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onTransactionChanged(TransactionChangedEvent event) {
        if(!event.isOverBudget())
            return;

        log.warn("Budget {} of family {} is overspent: {} spent of {} allowed",
                event.getBudgetId(), event.getFamilyId(), event.getBudgetSpent(), event.getBudgetLimit() );

        publisher.publish("budget.overspent",
                new BudgetOverspentEvent(event.getBudgetId(), event.getFamilyId(), event.getBudgetSpent(), event.getBudgetLimit(), Instant.now()));
    }
}
