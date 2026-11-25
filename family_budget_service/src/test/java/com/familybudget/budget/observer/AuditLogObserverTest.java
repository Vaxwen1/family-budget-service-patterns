package com.familybudget.budget.observer;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.familybudget.budget.event.BudgetProvisionedEvent;
import com.familybudget.budget.event.TransactionChangedEvent;
import com.familybudget.budget.event.enums.ChangeType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class AuditLogObserverTest {

    private final AuditLogObserver observer = new AuditLogObserver();
    private final Logger logger = (Logger) LoggerFactory.getLogger(AuditLogObserver.class);
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

    @BeforeEach
    void attachAppender() {
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void detachAppender() {
        logger.detachAppender(appender);
    }

    @Test
    void auditsATransactionEventWithItsRoutingKey() {
        observer.audit(new TransactionChangedEvent(
                ChangeType.UPDATED, 1L, 2L, 3L, 50L,
                new BigDecimal("25.00"), "Bread", LocalDate.of(2026, 11, 1),
                new BigDecimal("125.00"), new BigDecimal("500.00"), Instant.now()));

        assertThat(appender.list).hasSize(1);
        ILoggingEvent logged = appender.list.get(0);
        assertThat(logged.getLevel()).isEqualTo(Level.INFO);
        assertThat(logged.getFormattedMessage()).startsWith("Audit transaction.updated ->");
    }

    @Test
    void auditsABudgetProvisionedEventWithItsRoutingKey() {
        observer.audit(new BudgetProvisionedEvent(1L, 9L, "Monthly budget",
                new BigDecimal("800.00"), Instant.now()));

        assertThat(appender.list).hasSize(1);
        assertThat(appender.list.get(0).getFormattedMessage()).startsWith("Audit budget.created ->");
    }
}
