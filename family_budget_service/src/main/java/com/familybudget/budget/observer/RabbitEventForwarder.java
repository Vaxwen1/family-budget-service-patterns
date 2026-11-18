package com.familybudget.budget.observer;

import com.familybudget.budget.event.DomainEvent;
import com.familybudget.budget.messaging.publisher.DomainEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class RabbitEventForwarder {
    private final DomainEventPublisher publisher;
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void forward(DomainEvent event) {
        publisher.publish(event.routingKey(), event.toMessage());
    }

}
