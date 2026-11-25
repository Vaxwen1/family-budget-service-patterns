package com.familybudget.budget.controller;

import com.familybudget.budget.dto.form.BudgetForm;
import com.familybudget.budget.dto.form.FamilyForm;
import com.familybudget.budget.dto.form.TransactionForm;
import com.familybudget.budget.dto.view.BudgetSummary;
import com.familybudget.budget.dto.view.DashboardView;
import com.familybudget.budget.dto.view.FamilySummary;
import com.familybudget.budget.exception.FamilyNotFoundException;
import com.familybudget.budget.facade.BudgetDashboardFacade;
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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = DashboardController.class)
class DashboardControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private BudgetDashboardFacade facade;

    @Test
    void listsFamilies() throws Exception {
        when(facade.listFamilies()).thenReturn(List.of(new FamilySummary(1L, "SINGLE", 1)));

        mvc.perform(get("/api/v2/families"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].type").value("SINGLE"))
                .andExpect(jsonPath("$[0].memberCount").value(1));
    }

    @Test
    void listsFamilyTypes() throws Exception {
        when(facade.availableFamilyTypes()).thenReturn(List.of("COUPLE", "STANDARD"));

        mvc.perform(get("/api/v2/family-types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("COUPLE"))
                .andExpect(jsonPath("$[1]").value("STANDARD"));
    }

    @Test
    void registersAFamilyAndReturnsCreated() throws Exception {
        when(facade.registerFamily(any(FamilyForm.class))).thenReturn(new FamilySummary(3L, "COUPLE", 0));

        mvc.perform(post("/api/v2/families")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"COUPLE\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.type").value("COUPLE"));
    }

    @Test
    void rejectsABlankFamilyTypeWithFieldErrors() throws Exception {
        mvc.perform(post("/api/v2/families")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Validation failed"))
                .andExpect(jsonPath("$.errors.type").value("Family type is required"));

        verifyNoInteractions(facade);
    }

    @Test
    void rejectsMalformedJsonWithBadRequest() throws Exception {
        mvc.perform(post("/api/v2/families")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Malformed JSON request body"));

        verifyNoInteractions(facade);
    }

    @Test
    void returnsTheDashboard() throws Exception {
        when(facade.getDashboard(7L)).thenReturn(new DashboardView(7L, "COUPLE", 2,
                new BigDecimal("5200.00"), new BigDecimal("1500.00"), new BigDecimal("1050.50"),
                new BigDecimal("4149.50"), List.of()));

        mvc.perform(get("/api/v2/families/7/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.familyId").value(7))
                .andExpect(jsonPath("$.familyType").value("COUPLE"))
                .andExpect(jsonPath("$.memberCount").value(2))
                .andExpect(jsonPath("$.left").value(4149.50));
    }

    @Test
    void unknownFamilyReturnsNotFound() throws Exception {
        when(facade.getDashboard(999L)).thenThrow(new FamilyNotFoundException(999L));

        mvc.perform(get("/api/v2/families/999/dashboard"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void createsABudgetAndReturnsCreated() throws Exception {
        when(facade.createBudgetFromTemplate(eq(3L), any(BudgetForm.class))).thenReturn(new BudgetSummary(
                5L, "Monthly budget", new BigDecimal("800.00"), BigDecimal.ZERO, new BigDecimal("800.00"),
                BigDecimal.ZERO, BigDecimal.ZERO, false, List.of()));

        mvc.perform(post("/api/v2/families/3/budgets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"limitAmount\":800.00}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.name").value("Monthly budget"));
    }

    @Test
    void rejectsANegativeBudgetLimit() throws Exception {
        mvc.perform(post("/api/v2/families/3/budgets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"limitAmount\":-1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.limitAmount").value("Limit amount cannot be negative"));

        verifyNoInteractions(facade);
    }

    @Test
    void recordsATransactionAndReturnsCreated() throws Exception {
        doNothing().when(facade).recordTransaction(eq(1L), eq(2L), eq(3L), any(TransactionForm.class));

        mvc.perform(post("/api/v2/families/1/budgets/2/categories/3/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":12.50,\"description\":\"Bread\",\"date\":\"2026-11-19\"}"))
                .andExpect(status().isCreated());

        verify(facade).recordTransaction(eq(1L), eq(2L), eq(3L), any(TransactionForm.class));
    }

    @Test
    void rejectsAZeroTransactionAmount() throws Exception {
        mvc.perform(post("/api/v2/families/1/budgets/2/categories/3/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.amount").value("Amount must be greater than zero"));

        verifyNoInteractions(facade);
    }
}
