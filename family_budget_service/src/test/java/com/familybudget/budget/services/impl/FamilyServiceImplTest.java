package com.familybudget.budget.services.impl;

import com.familybudget.budget.dto.request.FamilyRequest;
import com.familybudget.budget.entity.Family;
import com.familybudget.budget.exception.FamilyNotFoundException;
import com.familybudget.budget.messaging.dto.FamilyCreatedEvent;
import com.familybudget.budget.messaging.dto.FamilyUpdatedEvent;
import com.familybudget.budget.messaging.publisher.DomainEventPublisher;
import com.familybudget.budget.repository.FamilyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
class FamilyServiceImplTest {

    @Mock private FamilyRepository familyRepository;
    @Mock private DomainEventPublisher publisher;

    @InjectMocks private FamilyServiceImpl service;

    @Test
    void createFamilyStoresTheTypeAndPublishesCreated() {
        when(familyRepository.save(any(Family.class))).thenAnswer(invocation -> {
            Family family = invocation.getArgument(0);
            family.setId(1L);
            return family;
        });
        FamilyRequest request = new FamilyRequest();
        request.setType("COUPLE");

        Family saved = service.createFamily(request);

        assertThat(saved.getId()).isEqualTo(1L);
        assertThat(saved.getType()).isEqualTo("COUPLE");
        assertThat(saved.getCreatedAt()).isNotNull();
        verify(publisher).publish(eq("family.created"), any(FamilyCreatedEvent.class));
    }

    @Test
    void getAllFamiliesReturnsEveryFamily() {
        List<Family> families = List.of(family(1L, "SINGLE"), family(2L, "COUPLE"));
        when(familyRepository.findAll()).thenReturn(families);

        assertThat(service.getAllFamilies()).isSameAs(families);
    }

    @Test
    void getFamilyByIdReturnsTheFamily() {
        Family family = family(1L, "SINGLE");
        when(familyRepository.findById(1L)).thenReturn(Optional.of(family));

        assertThat(service.getFamilyById(1L)).isSameAs(family);
    }

    @Test
    void getFamilyByIdFailsWhenMissing() {
        when(familyRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getFamilyById(1L))
                .isInstanceOf(FamilyNotFoundException.class);
    }

    @Test
    void updateFamilyChangesTheTypeAndPublishesUpdated() {
        Family existing = family(1L, "SINGLE");
        when(familyRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(familyRepository.save(any(Family.class))).thenAnswer(invocation -> invocation.getArgument(0));
        FamilyRequest request = new FamilyRequest();
        request.setType("FAMILY_WITH_CHILDREN");

        Family saved = service.updateFamily(1L, request);

        assertThat(saved.getType()).isEqualTo("FAMILY_WITH_CHILDREN");
        verify(publisher).publish(eq("family.updated"), any(FamilyUpdatedEvent.class));
    }

    @Test
    void updateFamilyKeepsTheTypeWhenNoneIsSent() {
        Family existing = family(1L, "SINGLE");
        when(familyRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(familyRepository.save(any(Family.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Family saved = service.updateFamily(1L, new FamilyRequest());

        assertThat(saved.getType()).isEqualTo("SINGLE");
    }

    @Test
    void updateFamilyFailsWhenMissingAndPublishesNothing() {
        when(familyRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateFamily(1L, new FamilyRequest()))
                .isInstanceOf(FamilyNotFoundException.class);

        verifyNoInteractions(publisher);
    }

    private static Family family(Long id, String type) {
        Family family = new Family();
        family.setId(id);
        family.setType(type);
        family.setCreatedAt(LocalDateTime.of(2026, 11, 1, 9, 0));
        return family;
    }
}
