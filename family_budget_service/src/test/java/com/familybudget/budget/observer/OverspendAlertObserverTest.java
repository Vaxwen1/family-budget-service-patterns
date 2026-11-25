package com.familybudget.budget.observer;

import com.familybudget.budget.event.TransactionChangedEvent;
import com.familybudget.budget.event.enums.ChangeType;
import com.familybudget.budget.messaging.dto.BudgetOverspentEvent;
import com.familybudget.budget.messaging.publisher.DomainEventPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class OverspendAlertObserverTest {

    @Mock private DomainEventPublisher publisher;

    @InjectMocks private OverspendAlertObserver observer;

    @Test
    void publishesAnOverspendMessageWhenTheLimitIsExceeded() {
        TransactionChangedEvent event = event("510.00", "500.00");

        observer.onTransactionChanged(event);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(publisher).publish(eq("budget.overspent"), captor.capture());
        BudgetOverspentEvent alert = (BudgetOverspentEvent) captor.getValue();
        assertThat(alert.familyId()).isEqualTo(1L);
        assertThat(alert.budgetId()).isEqualTo(2L);
        assertThat(alert.spent()).isEqualByComparingTo("510.00");
        assertThat(alert.limitAmount()).isEqualByComparingTo("500.00");
        assertThat(alert.occurredAt()).isNotNull();
    }

    @Test
    void staysSilentWhenSpendingIsBelowTheLimit() {
        observer.onTransactionChanged(event("250.00", "500.00"));

        verifyNoInteractions(publisher);
    }

    @Test
    void staysSilentWhenSpendingExactlyEqualsTheLimit() {
        observer.onTransactionChanged(event("500.00", "500.00"));

        verifyNoInteractions(publisher);
    }

    @Test
    void staysSilentWhenThereIsNoLimit() {
        observer.onTransactionChanged(event("40.00", null));

        verify(publisher, never()).publish(eq("budget.overspent"), any());
    }

    private static TransactionChangedEvent event(String spent, String limit) {
        return new TransactionChangedEvent(
                ChangeType.CREATED, 1L, 2L, 3L, 50L,
                new BigDecimal("25.00"), "Bread", LocalDate.of(2026, 11, 1),
                new BigDecimal(spent), limit == null ? null : new BigDecimal(limit),
                Instant.now());
    }
}
