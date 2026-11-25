package com.familybudget.budget.factory;

import com.familybudget.budget.entity.Budget;
import com.familybudget.budget.entity.Family;
import com.familybudget.budget.factory.creators.CoupleBudgetFactory;
import com.familybudget.budget.factory.creators.FamilyWithChildrenBudgetFactory;
import com.familybudget.budget.factory.creators.SingleBudgetFactory;
import com.familybudget.budget.factory.creators.StandardBudgetFactory;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BudgetFactoryTest {

    private final BudgetFactoryRegistry registry = new BudgetFactoryRegistry(List.of(
            new SingleBudgetFactory(),
            new CoupleBudgetFactory(),
            new FamilyWithChildrenBudgetFactory(),
            new StandardBudgetFactory()));

    @ParameterizedTest
    @CsvSource({
            "SINGLE, 4",
            "COUPLE, 5",
            "FAMILY_WITH_CHILDREN, 7",
            "STANDARD, 4"
    })
    void eachFactoryCreatesItsOwnCategoryList(String type, int expectedCategories) {
        Budget budget = registry.forFamilyType(type).createBudget(familyOf(type), new BigDecimal("1000.00"));

        assertThat(budget.getCategories()).hasSize(expectedCategories);
    }

    @ParameterizedTest
    @ValueSource(strings = {"SINGLE", "COUPLE", "FAMILY_WITH_CHILDREN", "STANDARD"})
    void newBudgetStartsWithZeroSpentAndLinksEverything(String type) {
        Family family = familyOf(type);

        Budget budget = registry.forFamilyType(type).createBudget(family, new BigDecimal("500.00"));

        assertThat(budget.getSpent()).isEqualByComparingTo("0");
        assertThat(budget.getLimitAmount()).isEqualByComparingTo("500.00");
        assertThat(budget.getFamily()).isSameAs(family);
        assertThat(budget.getCategories()).allSatisfy(category -> {
            assertThat(category.getBudget()).isSameAs(budget);
            assertThat(category.getSpent()).isEqualByComparingTo("0");
        });
    }

    private static Family familyOf(String type) {
        Family family = new Family();
        family.setType(type);
        return family;
    }
}
