package com.familybudget.budget.controller;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import com.familybudget.budget.exception.*;
import com.familybudget.budget.exception.SalaryNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;


@RestControllerAdvice(basePackages = "com.familybudget.budget.controller")
public class GlobalExceptionHandler {
    @ExceptionHandler({
        FamilyNotFoundException.class,
        BudgetNotFoundException.class,
        CategoryNotFoundException.class,
        TransactionNotFoundException.class,
        FamilyMemberNotFoundException.class,
        SalaryNotFoundException.class
    })
    public ProblemDetail handleNotFoundException(RuntimeException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        return problemDetail;
    }

    @ExceptionHandler (IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgumentException(IllegalArgumentException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        return problemDetail;
    }
}
