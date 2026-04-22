/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.math.BigDecimal;
import java.util.Currency;
import org.junit.jupiter.api.Test;

/**
 * {@code RV_EXCEPTION_NOT_THROWN} is suppressed class-wide because the constructor-validation tests deliberately invoke
 * {@code new BudgetExceededException(...)} inside an {@code assertThrows} lambda to verify the constructor itself
 * throws on invalid arguments (null budget, null spent, mixed currency). SpotBugs flags the created-but-not- thrown
 * exception without recognising the assertion pattern.
 *
 * @version ee
 */
@SuppressFBWarnings("RV_EXCEPTION_NOT_THROWN")
class BudgetExceededExceptionTest {

    private static final Currency EUR = Currency.getInstance("EUR");

    @Test
    void testConstructorRejectsNullBudget() {
        assertThrows(
            NullPointerException.class,
            () -> new BudgetExceededException("exceeded", null, Money.usd(BigDecimal.ONE)));
    }

    @Test
    void testConstructorRejectsNullSpent() {
        assertThrows(
            NullPointerException.class,
            () -> new BudgetExceededException("exceeded", Money.usd(BigDecimal.ONE), null));
    }

    @Test
    void testConstructorRejectsCurrencyMismatch() {
        // Mixed-currency budget/spent would silently produce wrong comparisons in downstream consumers that do
        // budget.subtract(spent); better to reject at the boundary than compute garbage.
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new BudgetExceededException(
                "exceeded", Money.usd(new BigDecimal("100.00")), Money.of(new BigDecimal("50.00"), EUR)));

        assertTrue(exception.getMessage()
            .contains("currency mismatch"));
    }

    @Test
    void testAccessorsReturnConstructorValues() {
        Money budget = Money.usd(new BigDecimal("100.00"));
        Money spent = Money.usd(new BigDecimal("101.50"));

        BudgetExceededException exception = new BudgetExceededException("over budget", budget, spent);

        assertEquals("over budget", exception.getMessage());
        assertEquals(budget, exception.getBudget());
        assertEquals(spent, exception.getSpent());
    }

    @Test
    void testBackCompatUsdAccessors() {
        BudgetExceededException exception = new BudgetExceededException(
            "exceeded", Money.usd(new BigDecimal("100.00")), Money.usd(new BigDecimal("101.50")));

        assertEquals(new BigDecimal("100.00"), exception.getBudgetUsd());
        assertEquals(new BigDecimal("101.50"), exception.getSpentUsd());
    }
}
