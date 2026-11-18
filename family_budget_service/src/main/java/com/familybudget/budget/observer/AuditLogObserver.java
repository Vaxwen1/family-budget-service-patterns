package com.familybudget.budget.observer;

import com.familybudget.budget.event.DomainEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
public class AuditLogObserver {
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void audit(DomainEvent event){
        log.info("Audit {} -> {}", event.routingKey(), event.toString());
    }

}
