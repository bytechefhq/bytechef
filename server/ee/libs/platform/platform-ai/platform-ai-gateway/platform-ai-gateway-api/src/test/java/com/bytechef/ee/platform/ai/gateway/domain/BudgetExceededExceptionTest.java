/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.ee.platform.ai.llm.usage.Money;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.math.BigDecimal;
import java.util.Currency;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
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
