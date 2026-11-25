package com.familybudget.budget.controller;

import com.familybudget.budget.entity.Budget;
import com.familybudget.budget.exception.BudgetNotFoundException;
import com.familybudget.budget.services.BudgetService;
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

@WebMvcTest(controllers = BudgetController.class)
class BudgetControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private BudgetService budgetService;

    @Test
    void createBudgetReturnsTheSavedBudget() throws Exception {
        when(budgetService.createBudget(eq(1L), any())).thenReturn(budget(5L, "Holiday", "1200.00"));

        mvc.perform(post("/budget/families/1/budgets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Holiday\",\"limitAmount\":1200.00}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.name").value("Holiday"))
                .andExpect(jsonPath("$.limitAmount").value(1200.00));
    }

    @Test
    void listBudgetsReturnsTheFamilyBudgets() throws Exception {
        when(budgetService.getAllBudgets(1L)).thenReturn(List.of(budget(5L, "Holiday", "1200.00")));

        mvc.perform(get("/budget/families/1/budgets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(5))
                .andExpect(jsonPath("$[0].name").value("Holiday"));
    }

    @Test
    void getBudgetReturnsOneBudget() throws Exception {
        when(budgetService.getBudget(1L, 5L)).thenReturn(budget(5L, "Holiday", "1200.00"));

        mvc.perform(get("/budget/families/1/budgets/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5));
    }

    @Test
    void getMissingBudgetReturnsA404ProblemDetail() throws Exception {
        when(budgetService.getBudget(1L, 9L)).thenThrow(new BudgetNotFoundException(9L));

        mvc.perform(get("/budget/families/1/budgets/9"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Budget not found with id: 9"));
    }

    @Test
    void updateBudgetReturnsTheUpdatedBudget() throws Exception {
        when(budgetService.updateBudget(eq(1L), eq(5L), any())).thenReturn(budget(5L, "Summer", "1500.00"));

        mvc.perform(put("/budget/families/1/budgets/5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Summer\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Summer"));
    }

    private static Budget budget(Long id, String name, String limit) {
        Budget budget = new Budget();
        budget.setId(id);
        budget.setName(name);
        budget.setLimitAmount(new BigDecimal(limit));
        budget.setSpent(BigDecimal.ZERO);
        return budget;
    }
}
