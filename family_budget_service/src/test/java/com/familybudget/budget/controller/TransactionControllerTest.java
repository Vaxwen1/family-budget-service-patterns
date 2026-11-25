package com.familybudget.budget.controller;

import com.familybudget.budget.entity.Category;
import com.familybudget.budget.entity.Transaction;
import com.familybudget.budget.exception.TransactionNotFoundException;
import com.familybudget.budget.services.TransactionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TransactionController.class)
class TransactionControllerTest {

    private static final String BASE = "/budget/families/1/budgets/2/categories/3/transactions";

    @Autowired
    private MockMvc mvc;

    @MockBean
    private TransactionService transactionService;

    @Test
    void addTransactionReturns201WithTheSavedTransaction() throws Exception {
        when(transactionService.addTransaction(eq(1L), eq(2L), eq(3L), any())).thenReturn(transaction(50L));

        mvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":25.50,\"description\":\"Bread\",\"date\":\"2026-11-01\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(50))
                .andExpect(jsonPath("$.categoryId").value(3));
    }

    @Test
    void listTransactionsReturnsTheCategoryTransactions() throws Exception {
        when(transactionService.getTransactions(1L, 2L, 3L)).thenReturn(List.of(transaction(50L)));

        mvc.perform(get(BASE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].description").value("Bread"));
    }

    @Test
    void getTransactionReturnsOneTransaction() throws Exception {
        when(transactionService.getTransaction(1L, 2L, 3L, 50L)).thenReturn(transaction(50L));

        mvc.perform(get(BASE + "/50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(50));
    }

    @Test
    void getMissingTransactionReturnsA404ProblemDetail() throws Exception {
        when(transactionService.getTransaction(1L, 2L, 3L, 9L)).thenThrow(new TransactionNotFoundException(9L));

        mvc.perform(get(BASE + "/9"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Transaction not found with id: 9"));
    }

    @Test
    void updateTransactionReturnsTheUpdatedTransaction() throws Exception {
        when(transactionService.updateTransaction(eq(1L), eq(2L), eq(3L), eq(50L), any()))
                .thenReturn(transaction(50L));

        mvc.perform(put(BASE + "/50")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":30.00}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(50));
    }

    @Test
    void addTransactionWithoutAmountReturns400WhenTheServiceRejectsIt() throws Exception {
        when(transactionService.addTransaction(eq(1L), eq(2L), eq(3L), any()))
                .thenThrow(new IllegalArgumentException("Transaction amount is required"));

        mvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Bread\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Transaction amount is required"));
    }

    private static Transaction transaction(Long id) {
        Category category = new Category();
        category.setId(3L);
        return Transaction.builder()
                .id(id)
                .amount(new BigDecimal("25.50"))
                .description("Bread")
                .date(LocalDate.of(2026, 11, 1))
                .category(category)
                .build();
    }
}
