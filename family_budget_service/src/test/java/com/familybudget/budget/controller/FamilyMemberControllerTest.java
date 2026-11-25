package com.familybudget.budget.controller;

import com.familybudget.budget.dto.response.FamilyMemberResponse;
import com.familybudget.budget.exception.FamilyMemberNotFoundException;
import com.familybudget.budget.services.FamilyMemberService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = FamilyMemberController.class)
class FamilyMemberControllerTest {

    private static final String BASE = "/budget/families/1/members";

    @Autowired
    private MockMvc mvc;

    @MockBean
    private FamilyMemberService familyMemberService;

    @Test
    void addMemberReturnsTheSavedMember() throws Exception {
        when(familyMemberService.addFamilyMember(eq(1L), any())).thenReturn(member(11L, 100L, "ADULT"));

        mvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":100,\"role\":\"ADULT\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(11))
                .andExpect(jsonPath("$.role").value("ADULT"));
    }

    @Test
    void listMembersReturnsTheFamilyMembers() throws Exception {
        when(familyMemberService.getFamilyMembers(1L)).thenReturn(List.of(member(11L, 100L, "ADULT")));

        mvc.perform(get(BASE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value(100));
    }

    @Test
    void getMemberReturnsOneMember() throws Exception {
        when(familyMemberService.getFamilyMemberById(1L, 11L)).thenReturn(member(11L, 100L, "ADULT"));

        mvc.perform(get(BASE + "/11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(11));
    }

    @Test
    void getMissingMemberReturnsA404ProblemDetail() throws Exception {
        when(familyMemberService.getFamilyMemberById(1L, 9L)).thenThrow(new FamilyMemberNotFoundException(9L));

        mvc.perform(get(BASE + "/9"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Family member not found with id = 9"));
    }

    @Test
    void updateMemberReturnsTheUpdatedMember() throws Exception {
        when(familyMemberService.updateFamilyMember(eq(1L), eq(11L), any())).thenReturn(member(11L, 100L, "TEEN"));

        mvc.perform(put(BASE + "/11")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"TEEN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("TEEN"));
    }

    private static FamilyMemberResponse member(Long id, Long userId, String role) {
        FamilyMemberResponse response = new FamilyMemberResponse();
        response.setId(id);
        response.setUserId(userId);
        response.setRole(role);
        return response;
    }
}
