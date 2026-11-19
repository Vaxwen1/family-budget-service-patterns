package com.familybudget.budget.dto.view;

import java.math.BigDecimal;

public record CategorySummary(Long id, String name, String color, BigDecimal spent) {}
