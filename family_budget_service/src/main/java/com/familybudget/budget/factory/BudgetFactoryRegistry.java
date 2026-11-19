package com.familybudget.budget.factory;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class BudgetFactoryRegistry {

    private static final String FALLBACK_TYPE = "STANDARD";
    private final Map<String, BudgetFactory>  budgetFactories;

    public BudgetFactoryRegistry(List<BudgetFactory> allFactories) {
        this.budgetFactories = allFactories.stream()
                .collect(Collectors.toMap(factory -> normalize(factory.supportedFamilyType()), Function.identity()));
        if (!budgetFactories.containsKey(FALLBACK_TYPE)) {
            throw new IllegalStateException("No budget factory for fallback type " + FALLBACK_TYPE);
        }
    }

    public BudgetFactory forFamilyType(String familyType) {
        return budgetFactories.getOrDefault(normalize(familyType), budgetFactories.get(FALLBACK_TYPE));
    }

    private static String normalize(String type) {
        if (type == null) {
            return "";
        }
        return type.trim().toUpperCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
    }

    public List<String> getSupportedFamilyTypes() {
        return budgetFactories.keySet().stream().sorted().toList();
    }
}
