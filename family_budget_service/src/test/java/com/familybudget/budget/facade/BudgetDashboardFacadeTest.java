package com.familybudget.budget.facade;

import com.familybudget.budget.dto.form.BudgetForm;
import com.familybudget.budget.dto.form.FamilyForm;
import com.familybudget.budget.dto.form.TransactionForm;
import com.familybudget.budget.dto.request.FamilyRequest;
import com.familybudget.budget.dto.request.TransactionRequest;
import com.familybudget.budget.dto.response.FamilyMemberResponse;
import com.familybudget.budget.dto.view.BudgetSummary;
import com.familybudget.budget.dto.view.DashboardView;
import com.familybudget.budget.dto.view.FamilySummary;
import com.familybudget.budget.entity.Budget;
import com.familybudget.budget.entity.Category;
import com.familybudget.budget.entity.Family;
import com.familybudget.budget.entity.Salary;
import com.familybudget.budget.factory.BudgetFactoryRegistry;
import com.familybudget.budget.services.BudgetProvisioningService;
import com.familybudget.budget.services.BudgetService;
import com.familybudget.budget.services.FamilyMemberService;
import com.familybudget.budget.services.FamilyService;
import com.familybudget.budget.services.SalaryService;
import com.familybudget.budget.services.TransactionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BudgetDashboardFacadeTest {

    @Mock private FamilyService familyService;
    @Mock private FamilyMemberService familyMemberService;
    @Mock private SalaryService salaryService;
    @Mock private BudgetService budgetService;
    @Mock private BudgetProvisioningService budgetProvisioningService;
    @Mock private TransactionService transactionService;
    @Mock private BudgetFactoryRegistry factoryRegistry;

    @InjectMocks private BudgetDashboardFacade facade;

    @Test
    void overspentBudgetIsFlaggedAndCappedBar() {
        BudgetSummary summary = singleBudgetSummary(budget(1L, "500.00", "530.00"));

        assertThat(summary.remaining()).isEqualByComparingTo("-30.00");
        assertThat(summary.percentUsed()).isEqualByComparingTo("106");
        assertThat(summary.barPercent()).isEqualByComparingTo("100");
        assertThat(summary.overspent()).isTrue();
    }

    @Test
    void underspentBudgetIsNotFlagged() {
        BudgetSummary summary = singleBudgetSummary(budget(1L, "1000.00", "250.00"));

        assertThat(summary.percentUsed()).isEqualByComparingTo("25");
        assertThat(summary.barPercent()).isEqualByComparingTo("25");
        assertThat(summary.overspent()).isFalse();
    }

    @Test
    void percentIsRoundedHalfUp() {
        BudgetSummary summary = singleBudgetSummary(budget(1L, "1000.00", "333.33"));

        assertThat(summary.percentUsed()).isEqualByComparingTo("33");
    }

    @Test
    void zeroLimitGivesZeroPercentAndNeverOverspent() {
        BudgetSummary summary = singleBudgetSummary(budget(1L, "0", "40.00"));

        assertThat(summary.percentUsed()).isEqualByComparingTo("0");
        assertThat(summary.barPercent()).isEqualByComparingTo("0");
        assertThat(summary.overspent()).isFalse();
    }

    @Test
    void nullAmountsCountAsZero() {
        Budget budget = budget(1L, null, null);

        BudgetSummary summary = singleBudgetSummary(budget);

        assertThat(summary.limitAmount()).isEqualByComparingTo("0");
        assertThat(summary.spent()).isEqualByComparingTo("0");
        assertThat(summary.remaining()).isEqualByComparingTo("0");
        assertThat(summary.overspent()).isFalse();
    }

    @Test
    void dashboardTotalsAddUpIncomeBudgetsAndSpending() {
        Family family = family(7L, "COUPLE");
        when(familyService.getFamilyById(7L)).thenReturn(family);
        when(familyMemberService.getFamilyMembers(7L)).thenReturn(List.of(mock(FamilyMemberResponse.class)));
        when(salaryService.getSalaries(7L)).thenReturn(List.of(salary("2800.00"), salary("2400.00")));
        when(budgetService.getAllBudgets(7L)).thenReturn(List.of(
                budget(1L, "1000.00", "950.50"),
                budget(2L, "500.00", "100.00")));

        DashboardView view = facade.getDashboard(7L);

        assertThat(view.familyId()).isEqualTo(7L);
        assertThat(view.familyType()).isEqualTo("COUPLE");
        assertThat(view.memberCount()).isEqualTo(1);
        assertThat(view.income()).isEqualByComparingTo("5200.00");
        assertThat(view.totalBudgeted()).isEqualByComparingTo("1500.00");
        assertThat(view.totalSpent()).isEqualByComparingTo("1050.50");
        assertThat(view.left()).isEqualByComparingTo("4149.50");
        assertThat(view.budgets()).hasSize(2);
    }

    @Test
    void listFamiliesCountsMembersPerFamily() {
        when(familyService.getAllFamilies()).thenReturn(List.of(family(1L, "SINGLE")));
        when(familyMemberService.getFamilyMembers(1L)).thenReturn(List.of(
                mock(FamilyMemberResponse.class), mock(FamilyMemberResponse.class)));

        List<FamilySummary> families = facade.listFamilies();

        assertThat(families).containsExactly(new FamilySummary(1L, "SINGLE", 2));
    }

    @Test
    void availableFamilyTypesComesFromTheRegistry() {
        when(factoryRegistry.getSupportedFamilyTypes()).thenReturn(List.of("SINGLE", "STANDARD"));

        assertThat(facade.availableFamilyTypes()).containsExactly("SINGLE", "STANDARD");
    }

    @Test
    void registerFamilySendsTheTypeToTheService() {
        Family saved = family(9L, "SINGLE");
        when(familyService.createFamily(any(FamilyRequest.class))).thenReturn(saved);
        FamilyForm form = new FamilyForm();
        form.setType("SINGLE");

        FamilySummary result = facade.registerFamily(form);

        ArgumentCaptor<FamilyRequest> captor = ArgumentCaptor.forClass(FamilyRequest.class);
        verify(familyService).createFamily(captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo("SINGLE");
        assertThat(result).isEqualTo(new FamilySummary(9L, "SINGLE", 0));
    }

    @Test
    void createBudgetFromTemplateDelegatesToProvisioning() {
        when(budgetProvisioningService.provisionBudget(eq(3L), any(BigDecimal.class)))
                .thenReturn(budget(5L, "800.00", "0"));
        BudgetForm form = new BudgetForm();
        form.setLimitAmount(new BigDecimal("800.00"));

        BudgetSummary result = facade.createBudgetFromTemplate(3L, form);

        assertThat(result.id()).isEqualTo(5L);
        assertThat(result.limitAmount()).isEqualByComparingTo("800.00");
    }

    @Test
    void recordTransactionDefaultsTheDateToToday() {
        TransactionForm form = new TransactionForm();
        form.setAmount(new BigDecimal("12.50"));
        form.setDescription("Bread");

        facade.recordTransaction(1L, 2L, 3L, form);

        ArgumentCaptor<TransactionRequest> captor = ArgumentCaptor.forClass(TransactionRequest.class);
        verify(transactionService).addTransaction(eq(1L), eq(2L), eq(3L), captor.capture());
        assertThat(captor.getValue().getAmount()).isEqualByComparingTo("12.50");
        assertThat(captor.getValue().getDescription()).isEqualTo("Bread");
        assertThat(captor.getValue().getDate()).isEqualTo(LocalDate.now());
    }

    @Test
    void recordTransactionKeepsAnExplicitDate() {
        TransactionForm form = new TransactionForm();
        form.setAmount(new BigDecimal("1.00"));
        form.setDate(LocalDate.of(2026, 11, 1));

        facade.recordTransaction(1L, 2L, 3L, form);

        ArgumentCaptor<TransactionRequest> captor = ArgumentCaptor.forClass(TransactionRequest.class);
        verify(transactionService).addTransaction(eq(1L), eq(2L), eq(3L), captor.capture());
        assertThat(captor.getValue().getDate()).isEqualTo(LocalDate.of(2026, 11, 1));
    }

    private BudgetSummary singleBudgetSummary(Budget budget) {
        when(familyService.getFamilyById(1L)).thenReturn(family(1L, "SINGLE"));
        when(budgetService.getAllBudgets(1L)).thenReturn(List.of(budget));
        return facade.getDashboard(1L).budgets().get(0);
    }

    private static Budget budget(Long id, String limit, String spent) {
        Budget budget = new Budget();
        budget.setId(id);
        budget.setName("Monthly budget");
        budget.setLimitAmount(limit == null ? null : new BigDecimal(limit));
        budget.setSpent(spent == null ? null : new BigDecimal(spent));
        Category category = new Category();
        category.setId(id * 10);
        category.setName("Groceries");
        category.setColor("#4CAF50");
        category.setSpent(BigDecimal.ZERO);
        budget.setCategories(new ArrayList<>(List.of(category)));
        return budget;
    }

    private static Family family(Long id, String type) {
        Family family = new Family();
        family.setId(id);
        family.setType(type);
        return family;
    }

    private static Salary salary(String amount) {
        Salary salary = new Salary();
        salary.setAmount(new BigDecimal(amount));
        return salary;
    }
}
