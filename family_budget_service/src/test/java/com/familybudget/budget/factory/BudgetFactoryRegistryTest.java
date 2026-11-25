package com.familybudget.budget.factory;

import com.familybudget.budget.factory.creators.CoupleBudgetFactory;
import com.familybudget.budget.factory.creators.FamilyWithChildrenBudgetFactory;
import com.familybudget.budget.factory.creators.SingleBudgetFactory;
import com.familybudget.budget.factory.creators.StandardBudgetFactory;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BudgetFactoryRegistryTest {

    private final BudgetFactoryRegistry registry = new BudgetFactoryRegistry(List.of(
            new SingleBudgetFactory(),
            new CoupleBudgetFactory(),
            new FamilyWithChildrenBudgetFactory(),
            new StandardBudgetFactory()));

    @Test
    void picksTheFactoryForAnExactType() {
        assertThat(registry.forFamilyType("COUPLE")).isInstanceOf(CoupleBudgetFactory.class);
    }

    @Test
    void normalisesTextBeforeMatching() {
        assertThat(registry.forFamilyType("  family with children "))
                .isInstanceOf(FamilyWithChildrenBudgetFactory.class);
    }

    @Test
    void fallsBackToStandardForUnknownOrMissingTypes() {
        assertThat(registry.forFamilyType("UNKNOWN")).isInstanceOf(StandardBudgetFactory.class);
        assertThat(registry.forFamilyType(null)).isInstanceOf(StandardBudgetFactory.class);
    }

    @Test
    void failsAtStartupWithoutAStandardFactory() {
        assertThatThrownBy(() -> new BudgetFactoryRegistry(List.of(new SingleBudgetFactory())))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void listsTheSupportedTypesInOrder() {
        assertThat(registry.getSupportedFamilyTypes())
                .containsExactly("COUPLE", "FAMILY_WITH_CHILDREN", "SINGLE", "STANDARD");
    }
}
