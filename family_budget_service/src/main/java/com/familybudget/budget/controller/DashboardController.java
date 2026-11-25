package com.familybudget.budget.controller;

import com.familybudget.budget.dto.form.BudgetForm;
import com.familybudget.budget.dto.form.FamilyForm;
import com.familybudget.budget.dto.form.TransactionForm;
import com.familybudget.budget.dto.view.BudgetSummary;
import com.familybudget.budget.dto.view.DashboardView;
import com.familybudget.budget.dto.view.FamilySummary;
import com.familybudget.budget.facade.BudgetDashboardFacade;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v2")
@RequiredArgsConstructor
public class DashboardController {

    private final BudgetDashboardFacade facade;

    @GetMapping("/families")
    public List<FamilySummary> listFamilies() {
        return facade.listFamilies();
    }

    @GetMapping("/family-types")
    public List<String> familyTypes() {
        return facade.availableFamilyTypes();
    }

    @PostMapping("/families")
    @ResponseStatus(HttpStatus.CREATED)
    public FamilySummary registerFamily(@Valid @RequestBody FamilyForm form) {
        return facade.registerFamily(form);
    }

    @GetMapping("/families/{familyId}/dashboard")
    public DashboardView dashboard(@PathVariable Long familyId) {
        return facade.getDashboard(familyId);
    }

    @PostMapping("/families/{familyId}/budgets")
    @ResponseStatus(HttpStatus.CREATED)
    public BudgetSummary createBudget(@PathVariable Long familyId, @Valid @RequestBody BudgetForm form) {
        return facade.createBudgetFromTemplate(familyId, form);
    }

    @PostMapping("/families/{familyId}/budgets/{budgetId}/categories/{categoryId}/transactions")
    @ResponseStatus(HttpStatus.CREATED)
    public void recordTransaction(@PathVariable Long familyId, @PathVariable Long budgetId,
                                  @PathVariable Long categoryId, @Valid @RequestBody TransactionForm form) {
        facade.recordTransaction(familyId, budgetId, categoryId, form);
    }
}
