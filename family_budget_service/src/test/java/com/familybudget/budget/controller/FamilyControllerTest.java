package com.familybudget.budget.controller;

import com.familybudget.budget.entity.Family;
import com.familybudget.budget.exception.FamilyNotFoundException;
import com.familybudget.budget.services.FamilyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = FamilyController.class)
class FamilyControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private FamilyService familyService;

    @Test
    void createFamilyReturnsTheSavedFamily() throws Exception {
        when(familyService.createFamily(any())).thenReturn(family(1L, "COUPLE"));

        mvc.perform(post("/budget/families")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"COUPLE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.type").value("COUPLE"));
    }

    @Test
    void listFamiliesReturnsEveryFamily() throws Exception {
        when(familyService.getAllFamilies()).thenReturn(List.of(family(1L, "SINGLE"), family(2L, "COUPLE")));

        mvc.perform(get("/budget/families"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[1].type").value("COUPLE"));
    }

    @Test
    void getFamilyReturnsOneFamily() throws Exception {
        when(familyService.getFamilyById(1L)).thenReturn(family(1L, "SINGLE"));

        mvc.perform(get("/budget/families/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("SINGLE"));
    }

    @Test
    void getMissingFamilyReturnsA404ProblemDetail() throws Exception {
        when(familyService.getFamilyById(9L)).thenThrow(new FamilyNotFoundException(9L));

        mvc.perform(get("/budget/families/9"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Family not found with id: 9"));
    }

    @Test
    void updateFamilyReturnsTheUpdatedFamily() throws Exception {
        when(familyService.updateFamily(eq(1L), any())).thenReturn(family(1L, "FAMILY_WITH_CHILDREN"));

        mvc.perform(put("/budget/families/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"FAMILY_WITH_CHILDREN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("FAMILY_WITH_CHILDREN"));
    }

    private static Family family(Long id, String type) {
        Family family = new Family();
        family.setId(id);
        family.setType(type);
        family.setCreatedAt(LocalDateTime.of(2026, 11, 1, 9, 0));
        return family;
    }
}
