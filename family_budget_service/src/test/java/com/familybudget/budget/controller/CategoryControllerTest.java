package com.familybudget.budget.controller;

import com.familybudget.budget.entity.Category;
import com.familybudget.budget.exception.CategoryNotFoundException;
import com.familybudget.budget.services.CategoryService;
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

@WebMvcTest(controllers = CategoryController.class)
class CategoryControllerTest {

    private static final String BASE = "/budget/families/1/budgets/2/categories";

    @Autowired
    private MockMvc mvc;

    @MockBean
    private CategoryService categoryService;

    @Test
    void createCategoryReturnsTheSavedCategory() throws Exception {
        when(categoryService.addCategory(eq(1L), eq(2L), any())).thenReturn(category(3L, "Groceries", "#4CAF50"));

        mvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Groceries\",\"color\":\"#4CAF50\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.color").value("#4CAF50"));
    }

    @Test
    void listCategoriesReturnsTheBudgetCategories() throws Exception {
        when(categoryService.getCategories(1L, 2L)).thenReturn(List.of(category(3L, "Groceries", "#4CAF50")));

        mvc.perform(get(BASE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Groceries"));
    }

    @Test
    void getCategoryReturnsOneCategory() throws Exception {
        when(categoryService.getCategory(1L, 2L, 3L)).thenReturn(category(3L, "Groceries", "#4CAF50"));

        mvc.perform(get(BASE + "/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3));
    }

    @Test
    void getMissingCategoryReturnsA404ProblemDetail() throws Exception {
        when(categoryService.getCategory(1L, 2L, 9L)).thenThrow(new CategoryNotFoundException(9L));

        mvc.perform(get(BASE + "/9"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Category not found: 9"));
    }

    @Test
    void updateCategoryReturnsTheUpdatedCategory() throws Exception {
        when(categoryService.updateCategory(eq(1L), eq(2L), eq(3L), any()))
                .thenReturn(category(3L, "Food", "#00FF00"));

        mvc.perform(put(BASE + "/3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Food\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Food"));
    }

    private static Category category(Long id, String name, String color) {
        Category category = new Category();
        category.setId(id);
        category.setName(name);
        category.setColor(color);
        category.setSpent(BigDecimal.ZERO);
        return category;
    }
}
