package com.familybudget.budget.services.impl;

import com.familybudget.budget.dto.request.SalaryRequest;
import com.familybudget.budget.entity.Family;
import com.familybudget.budget.entity.Salary;
import com.familybudget.budget.exception.FamilyNotFoundException;
import com.familybudget.budget.exception.SalaryNotFoundException;
import com.familybudget.budget.messaging.dto.SalaryCreatedEvent;
import com.familybudget.budget.messaging.dto.SalaryUpdatedEvent;
import com.familybudget.budget.messaging.publisher.DomainEventPublisher;
import com.familybudget.budget.repository.FamilyRepository;
import com.familybudget.budget.repository.SalaryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SalaryServiceImplTest {

    @Mock private SalaryRepository salaryRepository;
    @Mock private FamilyRepository familyRepository;
    @Mock private DomainEventPublisher publisher;

    @InjectMocks private SalaryServiceImpl service;

    private Family family;

    @BeforeEach
    void setUp() {
        family = new Family();
        family.setId(1L);
        family.setType("SINGLE");
    }

    @Test
    void createSalarySavesTheSalaryAndPublishesCreated() {
        when(familyRepository.findById(1L)).thenReturn(Optional.of(family));
        when(salaryRepository.save(any(Salary.class))).thenAnswer(invocation -> {
            Salary salary = invocation.getArgument(0);
            salary.setId(20L);
            return salary;
        });

        Salary saved = service.createSalary(1L, request(new BigDecimal("2100.00"), "EUR", "Employer"));

        assertThat(saved.getId()).isEqualTo(20L);
        assertThat(saved.getAmount()).isEqualByComparingTo("2100.00");
        assertThat(saved.getCurrency()).isEqualTo("EUR");
        assertThat(saved.getSource()).isEqualTo("Employer");
        assertThat(saved.getFamily()).isSameAs(family);
        verify(publisher).publish(eq("salary.created"), any(SalaryCreatedEvent.class));
    }

    @Test
    void createSalaryFailsWhenFamilyDoesNotExist() {
        when(familyRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createSalary(1L, request(BigDecimal.ONE, "EUR", "X")))
                .isInstanceOf(FamilyNotFoundException.class);

        verifyNoInteractions(salaryRepository, publisher);
    }

    @Test
    void getSalariesReturnsTheFamilySalaries() {
        List<Salary> salaries = List.of(salary(20L));
        when(familyRepository.findById(1L)).thenReturn(Optional.of(family));
        when(salaryRepository.findByFamilyId(1L)).thenReturn(salaries);

        assertThat(service.getSalaries(1L)).isSameAs(salaries);
    }

    @Test
    void getSalariesFailsWhenFamilyDoesNotExist() {
        when(familyRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getSalaries(1L))
                .isInstanceOf(FamilyNotFoundException.class);
    }

    @Test
    void getSalaryByIdReturnsTheSalary() {
        Salary salary = salary(20L);
        when(familyRepository.findById(1L)).thenReturn(Optional.of(family));
        when(salaryRepository.findByIdAndFamilyId(20L, 1L)).thenReturn(Optional.of(salary));

        assertThat(service.getSalaryById(1L, 20L)).isSameAs(salary);
    }

    @Test
    void getSalaryByIdFailsWhenMissing() {
        when(familyRepository.findById(1L)).thenReturn(Optional.of(family));
        when(salaryRepository.findByIdAndFamilyId(20L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getSalaryById(1L, 20L))
                .isInstanceOf(SalaryNotFoundException.class);
    }

    @Test
    void getSalaryByIdFailsWhenFamilyDoesNotExist() {
        when(familyRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getSalaryById(1L, 20L))
                .isInstanceOf(FamilyNotFoundException.class);
    }

    @Test
    void updateSalaryChangesEveryFieldThatIsSentAndPublishesUpdated() {
        Salary existing = salary(20L);
        LocalDateTime received = LocalDateTime.of(2026, 11, 1, 8, 0);
        when(familyRepository.findById(1L)).thenReturn(Optional.of(family));
        when(salaryRepository.findByIdAndFamilyId(20L, 1L)).thenReturn(Optional.of(existing));
        when(salaryRepository.save(any(Salary.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SalaryRequest request = new SalaryRequest();
        request.setAmount(new BigDecimal("2500.00"));
        request.setCurrency("USD");
        request.setReceivedAt(received);
        request.setSource("Bonus");

        Salary saved = service.updateSalary(1L, 20L, request);

        assertThat(saved.getAmount()).isEqualByComparingTo("2500.00");
        assertThat(saved.getCurrency()).isEqualTo("USD");
        assertThat(saved.getReceivedAt()).isEqualTo(received);
        assertThat(saved.getSource()).isEqualTo("Bonus");
        verify(publisher).publish(eq("salary.updated"), any(SalaryUpdatedEvent.class));
    }

    @Test
    void updateSalaryKeepsFieldsThatAreNotSent() {
        Salary existing = salary(20L);
        when(familyRepository.findById(1L)).thenReturn(Optional.of(family));
        when(salaryRepository.findByIdAndFamilyId(20L, 1L)).thenReturn(Optional.of(existing));
        when(salaryRepository.save(any(Salary.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Salary saved = service.updateSalary(1L, 20L, new SalaryRequest());

        assertThat(saved.getAmount()).isEqualByComparingTo("2100.00");
        assertThat(saved.getCurrency()).isEqualTo("EUR");
        assertThat(saved.getSource()).isEqualTo("Employer");
    }

    @Test
    void updateSalaryFailsWhenMissingAndPublishesNothing() {
        when(familyRepository.findById(1L)).thenReturn(Optional.of(family));
        when(salaryRepository.findByIdAndFamilyId(20L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateSalary(1L, 20L, new SalaryRequest()))
                .isInstanceOf(SalaryNotFoundException.class);

        verifyNoInteractions(publisher);
    }

    @Test
    void updateSalaryFailsWhenFamilyDoesNotExist() {
        when(familyRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateSalary(1L, 20L, new SalaryRequest()))
                .isInstanceOf(FamilyNotFoundException.class);

        verifyNoInteractions(salaryRepository, publisher);
    }

    private static Salary salary(Long id) {
        Salary salary = Salary.builder()
                .amount(new BigDecimal("2100.00"))
                .currency("EUR")
                .source("Employer")
                .build();
        salary.setId(id);
        return salary;
    }

    private static SalaryRequest request(BigDecimal amount, String currency, String source) {
        SalaryRequest request = new SalaryRequest();
        request.setAmount(amount);
        request.setCurrency(currency);
        request.setSource(source);
        return request;
    }
}
