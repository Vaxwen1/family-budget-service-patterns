package com.familybudget.budget.controller;

import com.familybudget.budget.entity.Family;
import com.familybudget.budget.entity.Salary;
import com.familybudget.budget.exception.SalaryNotFoundException;
import com.familybudget.budget.services.SalaryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SalaryController.class)
class SalaryControllerTest {

    private static final String BASE = "/budget/families/1/salary";

    @Autowired
    private MockMvc mvc;

    @MockBean
    private SalaryService salaryService;

    @Test
    void listSalariesReturnsTheFamilySalaries() throws Exception {
        when(salaryService.getSalaries(1L)).thenReturn(List.of(salary(20L, "2100.00")));

        mvc.perform(get(BASE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].amount").value(2100.00))
                .andExpect(jsonPath("$[0].familyId").value(1));
    }

    @Test
    void createSalaryReturnsTheSavedSalary() throws Exception {
        when(salaryService.createSalary(eq(1L), any())).thenReturn(salary(20L, "2100.00"));

        mvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":2100.00,\"currency\":\"EUR\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(20))
                .andExpect(jsonPath("$.currency").value("EUR"));
    }

    @Test
    void getSalaryReturnsOneSalary() throws Exception {
        when(salaryService.getSalaryById(1L, 20L)).thenReturn(salary(20L, "2100.00"));

        mvc.perform(get(BASE + "/20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(20));
    }

    @Test
    void getMissingSalaryReturnsA404ProblemDetail() throws Exception {
        when(salaryService.getSalaryById(1L, 9L)).thenThrow(new SalaryNotFoundException(9L));

        mvc.perform(get(BASE + "/9"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Salary not found with id = 9"));
    }

    @Test
    void updateSalaryReturnsTheUpdatedSalary() throws Exception {
        when(salaryService.updateSalary(eq(1L), eq(20L), any())).thenReturn(salary(20L, "2500.00"));

        mvc.perform(put(BASE + "/20")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":2500.00}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(2500.00));
    }

    private static Salary salary(Long id, String amount) {
        Family family = new Family();
        family.setId(1L);
        family.setType("SINGLE");
        return Salary.builder()
                .id(id)
                .family(family)
                .amount(new BigDecimal(amount))
                .currency("EUR")
                .source("Employer")
                .build();
    }
}
