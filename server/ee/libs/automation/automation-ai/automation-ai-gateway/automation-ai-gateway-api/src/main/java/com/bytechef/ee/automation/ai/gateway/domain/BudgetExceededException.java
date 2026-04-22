/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.domain;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * Thrown by pre-request budget enforcement when the workspace's cumulative spend has reached the hard cap. Carries a
 * {@link Money}-typed breakdown so callers cannot mistake the currency of the values.
 *
 * <p>
 * Both {@code budget} and {@code spent} are required and must share a currency — an earlier single-arg constructor that
 * left both fields {@code null} was a silent escape hatch that defeated the type-safe breakdown.
 *
 * <p>
 * {@code CT_CONSTRUCTOR_THROW} is suppressed: the constructor's null and currency-mismatch validation is load-bearing —
 * silently accepting invalid combinations would defeat the whole point of the type-safe breakdown fields.
 *
 * @version ee
 */
@SuppressFBWarnings("CT_CONSTRUCTOR_THROW")
public class BudgetExceededException extends RuntimeException {

    private final Money budget;
    private final Money spent;

    public BudgetExceededException(String message, Money budget, Money spent) {
        super(message);

        Objects.requireNonNull(budget, "budget must not be null");
        Objects.requireNonNull(spent, "spent must not be null");

        if (!budget.currency()
            .equals(spent.currency())) {

            throw new IllegalArgumentException(
                "budget/spent currency mismatch: " + budget.currency()
                    .getCurrencyCode() + " vs "
                    + spent.currency()
                        .getCurrencyCode());
        }

        this.budget = budget;
        this.spent = spent;
    }

    public Money getBudget() {
        return budget;
    }

    public Money getSpent() {
        return spent;
    }

    /**
     * Back-compat accessor for REST clients that serialize the body as {@code budgetUsd}. Returns the numeric amount
     * only — the currency code is available via {@link #getBudget()}.
     */
    public BigDecimal getBudgetUsd() {
        return budget.amount();
    }

    /**
     * Back-compat accessor for REST clients that serialize the body as {@code spentUsd}.
     */
    public BigDecimal getSpentUsd() {
        return spent.amount();
    }
}
