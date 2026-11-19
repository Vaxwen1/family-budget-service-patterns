package com.familybudget.budget.facade;

import com.familybudget.budget.dto.form.BudgetForm;
import com.familybudget.budget.dto.form.FamilyForm;
import com.familybudget.budget.dto.form.TransactionForm;
import com.familybudget.budget.dto.request.FamilyRequest;
import com.familybudget.budget.dto.request.TransactionRequest;
import com.familybudget.budget.dto.view.BudgetSummary;
import com.familybudget.budget.dto.view.CategorySummary;
import com.familybudget.budget.dto.view.DashboardView;
import com.familybudget.budget.dto.view.FamilySummary;
import com.familybudget.budget.entity.Budget;
import com.familybudget.budget.entity.Family;
import com.familybudget.budget.factory.BudgetFactoryRegistry;
import com.familybudget.budget.services.BudgetProvisioningService;
import com.familybudget.budget.services.BudgetService;
import com.familybudget.budget.services.FamilyMemberService;
import com.familybudget.budget.services.FamilyService;
import com.familybudget.budget.services.SalaryService;
import com.familybudget.budget.services.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class BudgetDashboardFacade {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final FamilyService familyService;
    private final FamilyMemberService familyMemberService;
    private final SalaryService salaryService;
    private final BudgetService budgetService;
    private final BudgetProvisioningService budgetProvisioningService;
    private final TransactionService transactionService;
    private final BudgetFactoryRegistry factoryRegistry;

    public List<FamilySummary> listFamilies() {
        return familyService.getAllFamilies().stream()
                .map(family -> new FamilySummary(family.getId(), family.getType(),
                        familyMemberService.getFamilyMembers(family.getId()).size()))
                .toList();
    }

    public List<String> availableFamilyTypes() {
        return factoryRegistry.getSupportedFamilyTypes();
    }

    public DashboardView getDashboard(Long familyId) {
        Family family = familyService.getFamilyById(familyId);
        int memberCount = familyMemberService.getFamilyMembers(familyId).size();

        BigDecimal income = salaryService.getSalaries(familyId).stream()
                .map(salary -> nz(salary.getAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<BudgetSummary> budgets = budgetService.getAllBudgets(familyId).stream()
                .map(this::toBudgetSummary)
                .toList();

        BigDecimal totalBudgeted = budgets.stream()
                .map(BudgetSummary::limitAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalSpent = budgets.stream()
                .map(BudgetSummary::spent)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new DashboardView(family.getId(), family.getType(), memberCount, income,
                totalBudgeted, totalSpent, income.subtract(totalSpent), budgets);
    }

    @Transactional
    public FamilySummary registerFamily(FamilyForm form) {
        FamilyRequest request = new FamilyRequest();
        request.setType(form.getType());
        Family family = familyService.createFamily(request);
        return new FamilySummary(family.getId(), family.getType(), 0);
    }

    @Transactional
    public BudgetSummary createBudgetFromTemplate(Long familyId, BudgetForm form) {
        Budget budget = budgetProvisioningService.provisionBudget(familyId, form.getLimitAmount());
        return toBudgetSummary(budget);
    }

    @Transactional
    public void recordTransaction(Long familyId, Long budgetId, Long categoryId, TransactionForm form) {
        TransactionRequest request = new TransactionRequest();
        request.setAmount(form.getAmount());
        request.setDescription(form.getDescription());
        request.setDate(form.getDate() != null ? form.getDate() : LocalDate.now());
        transactionService.addTransaction(familyId, budgetId, categoryId, request);
    }

    private BudgetSummary toBudgetSummary(Budget budget) {
        BigDecimal limit = nz(budget.getLimitAmount());
        BigDecimal spent = nz(budget.getSpent());
        BigDecimal remaining = limit.subtract(spent);

        BigDecimal percentUsed = limit.signum() == 0
                ? BigDecimal.ZERO
                : spent.multiply(HUNDRED).divide(limit, 0, RoundingMode.HALF_UP);
        BigDecimal barPercent = percentUsed.max(BigDecimal.ZERO).min(HUNDRED);
        boolean overspent = limit.signum() > 0 && spent.compareTo(limit) > 0;

        List<CategorySummary> categories = budget.getCategories().stream()
                .map(category -> new CategorySummary(category.getId(), category.getName(),
                        category.getColor(), nz(category.getSpent())))
                .toList();

        return new BudgetSummary(budget.getId(), budget.getName(), limit, spent, remaining,
                percentUsed, barPercent, overspent, categories);
    }

    private static BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
