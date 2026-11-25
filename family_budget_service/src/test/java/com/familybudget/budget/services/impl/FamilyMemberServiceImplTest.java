package com.familybudget.budget.services.impl;

import com.familybudget.budget.dto.request.FamilyMemberRequest;
import com.familybudget.budget.dto.response.FamilyMemberResponse;
import com.familybudget.budget.entity.Family;
import com.familybudget.budget.entity.FamilyMember;
import com.familybudget.budget.exception.FamilyMemberNotFoundException;
import com.familybudget.budget.exception.FamilyNotFoundException;
import com.familybudget.budget.messaging.dto.FamilyMemberCreatedEvent;
import com.familybudget.budget.messaging.dto.FamilyMemberUpdatedEvent;
import com.familybudget.budget.messaging.publisher.DomainEventPublisher;
import com.familybudget.budget.repository.FamilyMemberRepository;
import com.familybudget.budget.repository.FamilyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
class FamilyMemberServiceImplTest {

    @Mock private FamilyRepository familyRepository;
    @Mock private FamilyMemberRepository familyMemberRepository;
    @Mock private DomainEventPublisher publisher;

    @InjectMocks private FamilyMemberServiceImpl service;

    private Family family;

    @BeforeEach
    void setUp() {
        family = new Family();
        family.setId(1L);
        family.setType("COUPLE");
    }

    @Test
    void addFamilyMemberSavesTheMemberAndPublishesCreated() {
        when(familyRepository.findById(1L)).thenReturn(Optional.of(family));
        when(familyMemberRepository.save(any(FamilyMember.class))).thenAnswer(invocation -> {
            FamilyMember member = invocation.getArgument(0);
            member.setId(11L);
            return member;
        });

        FamilyMemberResponse response = service.addFamilyMember(1L, request(100L, "ADULT"));

        assertThat(response.getId()).isEqualTo(11L);
        assertThat(response.getUserId()).isEqualTo(100L);
        assertThat(response.getRole()).isEqualTo("ADULT");
        verify(publisher).publish(eq("familyMember.created"), any(FamilyMemberCreatedEvent.class));
    }

    @Test
    void addFamilyMemberFailsWhenFamilyDoesNotExist() {
        when(familyRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.addFamilyMember(1L, request(100L, "ADULT")))
                .isInstanceOf(FamilyNotFoundException.class);

        verifyNoInteractions(familyMemberRepository, publisher);
    }

    @Test
    void getFamilyMembersMapsEachMemberToAResponse() {
        when(familyRepository.findById(1L)).thenReturn(Optional.of(family));
        when(familyMemberRepository.findByFamilyId(1L)).thenReturn(List.of(
                member(11L, 100L, "ADULT"),
                member(12L, 101L, "CHILD")));

        List<FamilyMemberResponse> result = service.getFamilyMembers(1L);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(11L);
        assertThat(result.get(1).getRole()).isEqualTo("CHILD");
    }

    @Test
    void getFamilyMembersFailsWhenFamilyDoesNotExist() {
        when(familyRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getFamilyMembers(1L))
                .isInstanceOf(FamilyNotFoundException.class);
    }

    @Test
    void getFamilyMemberByIdReturnsTheMember() {
        when(familyRepository.findById(1L)).thenReturn(Optional.of(family));
        when(familyMemberRepository.findByIdAndFamilyId(11L, 1L)).thenReturn(Optional.of(member(11L, 100L, "ADULT")));

        FamilyMemberResponse response = service.getFamilyMemberById(1L, 11L);

        assertThat(response.getId()).isEqualTo(11L);
        assertThat(response.getUserId()).isEqualTo(100L);
    }

    @Test
    void getFamilyMemberByIdFailsWhenTheMemberIsNotInThatFamily() {
        when(familyRepository.findById(1L)).thenReturn(Optional.of(family));
        when(familyMemberRepository.findByIdAndFamilyId(11L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getFamilyMemberById(1L, 11L))
                .isInstanceOf(FamilyMemberNotFoundException.class);
    }

    @Test
    void getFamilyMemberByIdFailsWhenFamilyDoesNotExist() {
        when(familyRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getFamilyMemberById(1L, 11L))
                .isInstanceOf(FamilyNotFoundException.class);
    }

    @Test
    void updateFamilyMemberChangesRoleAndUserIdAndPublishesUpdated() {
        FamilyMember existing = member(11L, 100L, "ADULT");
        when(familyRepository.findById(1L)).thenReturn(Optional.of(family));
        when(familyMemberRepository.findByIdAndFamilyId(11L, 1L)).thenReturn(Optional.of(existing));
        when(familyMemberRepository.save(any(FamilyMember.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FamilyMemberResponse response = service.updateFamilyMember(1L, 11L, request(200L, "TEEN"));

        assertThat(response.getUserId()).isEqualTo(200L);
        assertThat(response.getRole()).isEqualTo("TEEN");
        verify(publisher).publish(eq("familyMember.updated"), any(FamilyMemberUpdatedEvent.class));
    }

    @Test
    void updateFamilyMemberKeepsFieldsThatAreNotSent() {
        FamilyMember existing = member(11L, 100L, "ADULT");
        when(familyRepository.findById(1L)).thenReturn(Optional.of(family));
        when(familyMemberRepository.findByIdAndFamilyId(11L, 1L)).thenReturn(Optional.of(existing));
        when(familyMemberRepository.save(any(FamilyMember.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FamilyMemberResponse response = service.updateFamilyMember(1L, 11L, request(null, null));

        assertThat(response.getUserId()).isEqualTo(100L);
        assertThat(response.getRole()).isEqualTo("ADULT");
    }

    @Test
    void updateFamilyMemberFailsWhenTheMemberIsMissing() {
        when(familyRepository.findById(1L)).thenReturn(Optional.of(family));
        when(familyMemberRepository.findByIdAndFamilyId(11L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateFamilyMember(1L, 11L, request(200L, "TEEN")))
                .isInstanceOf(FamilyMemberNotFoundException.class);

        verifyNoInteractions(publisher);
    }

    @Test
    void updateFamilyMemberFailsWhenFamilyDoesNotExist() {
        when(familyRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateFamilyMember(1L, 11L, request(200L, "TEEN")))
                .isInstanceOf(FamilyNotFoundException.class);

        verifyNoInteractions(familyMemberRepository, publisher);
    }

    private static FamilyMemberRequest request(Long userId, String role) {
        FamilyMemberRequest request = new FamilyMemberRequest();
        request.setUserId(userId);
        request.setRole(role);
        return request;
    }

    private static FamilyMember member(Long id, Long userId, String role) {
        FamilyMember member = FamilyMember.builder().userId(userId).role(role).build();
        member.setId(id);
        return member;
    }
}
