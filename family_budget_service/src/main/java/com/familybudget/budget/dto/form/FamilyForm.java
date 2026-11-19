package com.familybudget.budget.dto.form;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FamilyForm {
    @NotBlank(message = "Family type is required")
    private String type;
}
