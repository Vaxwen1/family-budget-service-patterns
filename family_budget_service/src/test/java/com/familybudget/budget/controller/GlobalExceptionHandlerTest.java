package com.familybudget.budget.controller;

import com.familybudget.budget.exception.BudgetNotFoundException;
import com.familybudget.budget.exception.CategoryNotFoundException;
import com.familybudget.budget.exception.FamilyMemberNotFoundException;
import com.familybudget.budget.exception.FamilyNotFoundException;
import com.familybudget.budget.exception.SalaryNotFoundException;
import com.familybudget.budget.exception.TransactionNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.core.MethodParameter;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    static Stream<RuntimeException> notFoundExceptions() {
        return Stream.of(
                new FamilyNotFoundException(1L),
                new BudgetNotFoundException(2L),
                new CategoryNotFoundException(3L),
                new TransactionNotFoundException(4L),
                new FamilyMemberNotFoundException(5L),
                new SalaryNotFoundException(6L));
    }

    @ParameterizedTest
    @MethodSource("notFoundExceptions")
    void everyNotFoundExceptionBecomesA404WithItsMessage(RuntimeException ex) {
        ProblemDetail problem = handler.handleNotFoundException(ex);

        assertThat(problem.getStatus()).isEqualTo(404);
        assertThat(problem.getDetail()).isEqualTo(ex.getMessage());
    }

    @Test
    void illegalArgumentBecomesA400WithItsMessage() {
        ProblemDetail problem = handler.handleIllegalArgumentException(
                new IllegalArgumentException("Transaction amount is required"));

        assertThat(problem.getStatus()).isEqualTo(400);
        assertThat(problem.getDetail()).isEqualTo("Transaction amount is required");
    }

    @Test
    @SuppressWarnings("unchecked")
    void validationErrorsBecomeA400WithAFieldToMessageMap() throws NoSuchMethodException {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "form");
        bindingResult.addError(new FieldError("form", "amount", "Amount is required"));
        bindingResult.addError(new FieldError("form", "description", "Too long"));
        MethodParameter parameter = new MethodParameter(Object.class.getMethod("toString"), -1);

        ProblemDetail problem = handler.handleValidation(new MethodArgumentNotValidException(parameter, bindingResult));

        assertThat(problem.getStatus()).isEqualTo(400);
        assertThat(problem.getDetail()).isEqualTo("Validation failed");
        Map<String, String> errors = (Map<String, String>) problem.getProperties().get("errors");
        assertThat(errors)
                .containsEntry("amount", "Amount is required")
                .containsEntry("description", "Too long");
    }

    @Test
    void malformedJsonBecomesA400WithAFixedMessage() {
        ProblemDetail problem = handler.handleMalformedJson(
                new HttpMessageNotReadableException("unexpected character", new IllegalStateException("x")));

        assertThat(problem.getStatus()).isEqualTo(400);
        assertThat(problem.getDetail()).isEqualTo("Malformed JSON request body");
    }
}
