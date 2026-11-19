package com.familybudget.budget.dto.form;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class BudgetForm {

    @NotNull(message = "Limit amount is required")
    @DecimalMin(value = "0.00", message = "Limit amount cannot be negative")
    private BigDecimal limitAmount;
}
